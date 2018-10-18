package info.benjaminhill.nextbot.cloud

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.*
import mu.KLoggable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * Firestore cloud-synchronized map and properties
 * <code>var isOnline by delegate(initialValue = true, shutdownValue = false)</code>
 *
 * Observers are hard.  If you have a "shutdown" observer, and another "link left speed to motor0 speed" observer,
 * when you shut down the program both tries to happen at once.
 *
 * @param path [collection to documentID]+ location of doc to sync
 *
 * * can be a String, Double, or Boolean
 * * Has a range of allowed values and will veto if not in the range
 * * Protects against setting to the same value
 * * best-effort shutdown values
 * * observers alter properties in a batchy way by setting
 */
open class CloudSyncDoc(path: List<Pair<String, String>>) : AutoCloseable {

    private val db: Firestore = FirestoreOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(
                    ClassLoader.getSystemClassLoader().getResourceAsStream("serviceAccountKey.json")))
            .setTimestampsInSnapshotsEnabled(true)
            .build().service!!

    private val docRef: DocumentReference

    /** When the cloud tells you things, you should listen */
    private lateinit var updateFromCloudListener: ListenerRegistration

    /**
     * Immediately set the localState, cloud sync may take longer or be completely offline.
     * Should be only accessed through the CloudSyncDelegate, not in this class, or you miss all the protective logic and observers.
     */
    private val localState = mutableMapOf<String, Any>()

    /** Easy way to find the observer for a given property */
    private val docObservers = mutableMapOf<String, MutableList<() -> Unit>>()

    init {
        logger.info { "CloudSyncDoc:init" }
        docRef = path.also {
            require(it.isNotEmpty()) { "No documents in the root please." }
        }.drop(1).fold(db.collection(path.first().first).document(path.first().second)) { agg, nextPath ->
            agg.collection(nextPath.first).document(nextPath.second)
        }.also { logger.info { "Attached bot to ${it.path}" } }
    }

    /** Sync up defaults, listen for updates */
    fun startSync() {
        // All delegates should have entered their defaults into localState by now!
        println("localState before default setting:${localState.entries}")
        val missingDefaults = localState.minus(docRef.get().get()?.data?.keys ?: setOf())
        if (missingDefaults.isNotEmpty()) {
            logger.warn { "Setting missing defaults: ${missingDefaults.entries}" }
            missingDefaults.forEach { key, value ->
                this[key] = value
            }
            docRef.set(asSimpleMap(), SetOptions.merge()) // Include observer ripples as well.
        }

        updateFromCloudListener = docRef.addSnapshotListener { snapshot, e ->
            e?.let { throw IllegalStateException(e) }
            val updates = (snapshot?.data
                    ?: mapOf()).filter { (key, value) -> validateChange(key, localState[key], value) }
            logger.debug { "Cloud->Local update of ${updates.entries} (after filtering out unchanged state)" }
            updates.forEach { (key, value) ->
                // Set through here to trigger observers
                this[key] = value
            }
        }
    }

    fun addObserver(prop: KProperty<*>, fn: () -> Unit) {
        require(docObservers.contains(prop.name)) { "Tried to set observer for '${prop.name}' which isn't registered." }
        require(prop.instanceParameter != this) { "Tried to set observer on some random property" }
        docObservers[prop.name]!!.add(fn)
    }

    protected fun asSimpleMap() = this::class.memberProperties
            .filter { it.returnType.jvmErasure in setOf(Boolean::class, Double::class, String::class) }
            .map { it.name to this[it.name] }
            .toMap()

    /** Only way you should be setting values in the localState */
    protected operator fun set(key: String, value: Any) {
        @Suppress("UNCHECKED_CAST")
        (this::class.memberProperties
                .first { it is KMutableProperty1<*, *> && it.name == key }
                as KMutableProperty1<CloudSyncDoc, Any>).set(this, value)
    }

    /** Gets local value using reflection, because it can be a bit twitchy if trying to do this from inside a Coroutine Scope */
    @Suppress("UNCHECKED_CAST")
    operator fun get(key: String): Any? = (this::class.memberProperties.first { it.name == key } as KMutableProperty1<CloudSyncDoc, Any>).get(this)

    override fun close() {
        logger.info { "Closing" }
        updateFromCloudListener.remove() // no cascades
        db.close()
        logger.info { "Finished closing." }
    }

    /**
     * Interesting stuff can be set immediately.
     * https://stackoverflow.com/questions/52809157/how-to-combine-kotlin-delegated-property-observable-vetoable-and-by-map
     */
    open inner class CloudSyncDelegate<T : Any>(
            protected val initialValue: T
    ) : ReadWriteProperty<CloudSyncDoc, T> {

        init {
            require(initialValue is Boolean || initialValue is Number || initialValue is String)
        }

        /**
         * Like an init for the delegate.
         * https://kotlinlang.org/docs/reference/delegated-properties.html#providing-a-delegate-since-11
         */
        open operator fun provideDelegate(
                thisRef: CloudSyncDoc,
                prop: KProperty<*>
        ): CloudSyncDelegate<T> {
            logger.debug { "provideDelegate ${prop.name}, registering in doc" }
            prop.isAccessible = true // enable later access
            thisRef.localState[prop.name] = initialValue
            thisRef.docObservers[prop.name] = mutableListOf()
            return CloudSyncDelegate(initialValue)
        }

        override fun setValue(thisRef: CloudSyncDoc, property: KProperty<*>, value: T) {
            val oldValue = getValue(thisRef, property)
            if (validateChange(property.name, oldValue, value)) {
                logger.debug { "setValue(${property.name}) from $oldValue to $value, locally and cloud." }
                thisRef.localState[property.name] = value
                afterChange(property)
                thisRef.docRef.set(mapOf(property.name to value), SetOptions.merge()) // TODO: combine updates, get from observer change list
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: CloudSyncDoc, property: KProperty<*>): T =
                thisRef.localState[property.name] as T

        private fun afterChange(property: KProperty<*>) {
            // Don't have access to a thisRef.docObservers here, so need to use inner class.
            if (docObservers[property.name]!!.isNotEmpty()) {
                logger.debug { "Running ${docObservers[property.name]!!.size} observers on '${property.name}'" }
                docObservers[property.name]!!.forEach { it() }
            }
        }
    }


    companion object : KLoggable {
        override val logger = logger()
        /**
         * Ignore "set to (nearly) identical value"
         * Throw errors if doubles outside of -1.0..1.0
         */
        fun validateChange(name: String, oldValue: Any?, newValue: Any): Boolean {
            when (newValue) {
                is Boolean -> {
                    return when (newValue) {
                        (oldValue as Boolean) -> {
                            false
                        }
                        else -> true
                    }
                }
                is Double -> {
                    return when {
                        newValue !in -1.0..1.0 -> throw IllegalArgumentException("Not in range: $name=$newValue")
                        newValue == oldValue -> {
                            false
                        }
                        Math.abs(newValue.toDouble() - (oldValue as Double)) < Math.ulp(newValue.toDouble()) * 5 -> {
                            logger.debug { "Ignoring too-close Double values $name=($oldValue to $newValue)" }
                            false
                        }
                        else -> true
                    }
                }
                is String -> {
                    return when {
                        (oldValue as String).trim() == newValue.trim() -> {
                            return false
                        }
                        else -> true
                    }
                }
                else -> throw IllegalArgumentException("Unexpected value type: $name=$newValue")
            }
        }
    }


}


