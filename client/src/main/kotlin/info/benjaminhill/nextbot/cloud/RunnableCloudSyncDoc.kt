package info.benjaminhill.nextbot.cloud


import mu.KLoggable
import java.net.NetworkInterface
import kotlin.reflect.KProperty

/**
 * Adds an "isRunning:Boolean" and support for shutdownValue
 * Device ID based on MAC address, which isn't horrible.
 */
open class RunnableCloudSyncDoc(userId: String) : CloudSyncDoc(listOf("users" to userId, "devices" to macAddressId)) {

    /** Every document has a state for supporting shutdown observers. */
    var isRunning by CloudSyncDelegate(initialValue = false)

    override fun close() {
        isRunning = false // Triggers any shutdown observers
        super.close()
    }

    /** Extend the delegate "not-constructor" to install shutdown hook observers */
    open inner class RunnableCloudSyncDelegate<T : Any>(
            initialValue: T,
            private val shutdownValue: T? = null
    ) : CloudSyncDelegate<T>(initialValue) {

        /** Register shutdown hooks.  Don't call super "constructor" */
        override operator fun provideDelegate(
                thisRef: CloudSyncDoc,
                prop: KProperty<*>
        ): CloudSyncDelegate<T> {
            // Do the registration first
            val result = super.provideDelegate(thisRef, prop)

            shutdownValue?.let {
                logger.info { "Adding shutdown observer: ${prop.name}=$shutdownValue" }
                thisRef.addObserver(this@RunnableCloudSyncDoc::isRunning) {
                    if (!isRunning) {
                        logger.info { "isRunning=$isRunning: initiating shutdown observer ${prop.name}=$shutdownValue" }
                        setValue(thisRef, prop, shutdownValue)
                    }
                }
            }
            return result
        }
    }

    companion object : KLoggable {
        override val logger = logger()

        /** Not a pretty unique ID, but the one we have */
        val macAddressId: String by lazy {
            NetworkInterface.getNetworkInterfaces().toList()
                    .sortedBy { it.displayName }
                    .mapNotNull { it.hardwareAddress?.joinToString("") { b -> String.format("%02X", b) } }
                    .joinToString("")
        }
    }
}