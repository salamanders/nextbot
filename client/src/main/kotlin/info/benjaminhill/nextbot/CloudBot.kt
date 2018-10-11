package info.benjaminhill.nextbot


import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import mu.KLoggable
import java.io.Serializable
import java.net.NetworkInterface
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * Thin wrapper for synchronizing with the cloud
 *
 * When any change happens, if the value is different, all observers are notified
 * (regardless if the change originated locally or from the cloud)
 *
 * Supported Properties are String, Doubles (-1.0..1.0), or Booleans
 */
open class CloudBot(fbId: String, uId: String) : AutoCloseable, Serializable {

    var running by syncToCloud(true)

    /** Hook up devices here, which will read from the property if triggered. Also useful as an easy-access map of properties */
    @Transient
    protected lateinit var observers: Map<String, MutableSet<() -> Unit>>

    @Transient
    protected val dbRefState: DatabaseReference

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(
                        ClassLoader.getSystemClassLoader().getResourceAsStream("serviceAccountKey.json")))
                .setDatabaseUrl("https://$fbId.firebaseio.com")
                .build()!!)

        // TODO: Migrate to firestore
        // Ref to this particular client's current state
        dbRefState = FirebaseDatabase.getInstance().getReference("users/$uId/devices/$macAddressId/state")
        logger.info { "Attached bot to $dbRefState" }


        // Set local properties when Cloud properties change
        dbRefState.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) =
                    logger.error(databaseError.toException()) { "Should never onCancelled" }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.filterNotNull().forEach { child ->
                    setValue(child.key!!, child.value!!)
                }
            }
        })

        // TODO: Cloud state should win by default, unless explicitly overridden by the bot... which is hard, because empty bot state needed to set the cloud state.
    }

    /**
     * Auto-syncs Client->Cloud.
     * Only does Client->Device through observers.
     * Cloud->Client is handled by init
     */
    protected fun <T : Any> syncToCloud(initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {

        override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
            logger.debug { "${property.name} beforeChange($newValue)" }
            return when (newValue) {
                is Boolean -> newValue != (oldValue as Boolean)
                is Number -> {
                    if (newValue.toDouble() !in -1.0..1.0) {
                        logger.warn { "Rejecting double outside of range: ${property.name}=$newValue" }
                        return false
                    }
                    // Guard against double rounding errors
                    if (Math.abs(newValue.toDouble() - (oldValue as Double)) < Math.ulp(newValue.toDouble()) * 5) {
                        return false // silently ignore same-value to avoid spins
                    }
                    return true
                }
                is String -> return newValue.trim() != (oldValue as String).trim()
                else -> throw IllegalStateException("Unknown sync type ${newValue::class.qualifiedName}")
            }
        }

        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
            logger.debug { "afterChange ${property.name}=$newValue" }
            dbRefState.child(property.name).setValueAsync(newValue)
            if (this@CloudBot::observers.isInitialized) {
                observers[property.name]!!.forEach { it() }
            } else {
                logger.warn { "Tried to sync property ${property.name} before setting up initObserversAndSync()" }
            }
        }
    }

    /** Sets a value using reflection, useful for external setters */
    fun setValue(key: String, newValueAny: Any) {
        when (newValueAny) {
            is Number -> {
                @Suppress("UNCHECKED_CAST")
                val prop = this::class.memberProperties
                        .filter { it is KMutableProperty1<*, *> }
                        .first { it.name == key } as KMutableProperty1<CloudBot, Double>
                prop.set(this, newValueAny.toDouble())
            }
            is Boolean -> {
                @Suppress("UNCHECKED_CAST")
                val prop = this::class.memberProperties
                        .filter { it is KMutableProperty1<*, *> }
                        .first { it.name == key } as KMutableProperty1<CloudBot, Boolean>
                prop.set(this, newValueAny)
            }
            is String -> {
                @Suppress("UNCHECKED_CAST")
                val prop = this::class.memberProperties
                        .filter { it is KMutableProperty1<*, *> }
                        .first { it.name == key } as KMutableProperty1<CloudBot, String>
                prop.set(this, newValueAny)
            }
            else -> throw IllegalArgumentException("Unexpected value type: $newValueAny (${newValueAny::class})")
        }
    }

    /** Helper, because it can be a bit twitchy if trying to do this from inside a Coroutine Scope */
    fun getValue(key: String): Any? = this.javaClass.kotlin.memberProperties.first { it.name == key }.get(this)

    fun asSimpleMap() = this.javaClass.kotlin.memberProperties.filter { observers.containsKey(it.name) }
            .fold(mutableMapOf<String, Any?>()) { result, prop ->
                result[prop.name] = prop.get(this)
                result
            }.toMap()

    open fun initObserversAndSync() {
        // Send up default values in the cloud, in case they are used to render a UI.  Has to be done post-init.
        // TODO: Don't clobber stuff like scripts
        observers = this::class.memberProperties
                .filter { it.returnType.jvmErasure in setOf(Boolean::class, Double::class, String::class) }
                .associateBy({ it.name }, { mutableSetOf<() -> Unit>() })

        dbRefState.setValueAsync(this)
    }

    override fun close() {
        logger.info { "CloudBot shutting down." }
        running = false
        dbRefState.database.purgeOutstandingWrites()
        dbRefState.database.goOffline()
    }


    companion object : KLoggable {
        override val logger = logger()

        val macAddressId: String by lazy {
            NetworkInterface.getNetworkInterfaces().toList()
                    .sortedBy { it.displayName }
                    .mapNotNull { it.hardwareAddress?.joinToString("") { b -> String.format("%02X", b) } }
                    .joinToString("")
        }
    }
}