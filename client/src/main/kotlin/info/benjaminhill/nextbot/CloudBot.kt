package info.benjaminhill.nextbot

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import java.io.FileInputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.logging.Logger
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
 * Supported Properties are either Doubles (-1.0..1.0) or Booleans
 */
open class CloudBot(fbId: String, uId: String) : AutoCloseable, Serializable {
    @Transient
    protected val dbRefState: DatabaseReference

    @Transient
    private var log: Logger = Logger.getLogger(this::class.java.name)!!

    @Transient
    private val macAddressId:String = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
            .hardwareAddress.joinToString("") { String.format("%02X",it) }

    var active by syncToCloud(true)

    /** Hook up devices here, which will read from the property if triggered. */
    @Transient
    protected val observers: Map<String, MutableSet<() -> Unit>> = this@CloudBot::class.memberProperties
            .filter { it.returnType.jvmErasure in setOf(Boolean::class, Double::class) }
            .associateBy({ it.name }, { mutableSetOf<() -> Unit>() })

    /**
     * Auto-syncs Client->Cloud.
     * Only does Client->Device through observers.
     * Cloud->Client is handled by init
     */
    protected fun <T : Any> syncToCloud(initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {

        override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
            log.info("${property.name} beforeChange($newValue)")
            return when (newValue) {
                is Boolean -> newValue != (oldValue as Boolean)
                is Number -> {
                    if (newValue.toDouble() !in -1.0..1.0) {
                        log.warning("Rejecting double outside of range: ${property.name}=$newValue")
                        return false
                    }
                    // Guard against double rounding errors
                    if (Math.abs(newValue.toDouble() - (oldValue as Double)) < Math.ulp(newValue.toDouble()) * 5) {
                        return false // silently ignore same-value to avoid spins
                    }
                    return true
                }
                else -> throw IllegalStateException("Unknown sync type ${newValue::class.qualifiedName}")
            }
        }

        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
            log.info("afterChange ${property.name}=$newValue")
            dbRefState.child(property.name).setValueAsync(newValue)
            // Call all registered callbacks
            observers[property.name]!!.forEach { it() }
        }
    }


    init {

        val serviceAccount = FileInputStream("resources/serviceAccountKey.json")
        val options = FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://$fbId.firebaseio.com")
                .build()!!
        FirebaseApp.initializeApp(options)


        // TODO: Migrate to firestore
        // Ref to this particular client's current state
        dbRefState = FirebaseDatabase.getInstance().getReference("users/$uId/devices/$macAddressId/state")
        log.info("Attached bot to $dbRefState")

        // Set local properties when Cloud properties change
        dbRefState.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) = log.severe(databaseError.toString())

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.filterNotNull().forEach { child ->
                    val key = child.key!!
                    val newValueAny = child.value!!

                    when (newValueAny) {
                        is Number -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = this@CloudBot::class.memberProperties
                                    .filter { it is  KMutableProperty1<*, *> }
                                    .first { it.name == key } as KMutableProperty1<CloudBot, Double>
                            prop.set(this@CloudBot, newValueAny.toDouble())
                        }
                        is Boolean -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = this@CloudBot::class.memberProperties
                                    .filter { it is  KMutableProperty1<*, *> }
                                    .first { it.name == key } as KMutableProperty1<CloudBot, Boolean>
                            prop.set(this@CloudBot, newValueAny)
                        }
                        else -> throw IllegalArgumentException("Unexpected value type: $newValueAny (${newValueAny::class})")
                    }
                }
            }
        })
    }

    fun open() {
        // Send up default values in the cloud, in case they are used to render a UI.  Has to be done post-init.
        dbRefState.setValueAsync(this)
    }

    override fun close() {
        log.warning("CloudBot.close()")
        active = false
        FirebaseDatabase.getInstance().purgeOutstandingWrites()
        FirebaseDatabase.getInstance().goOffline()
    }


}
