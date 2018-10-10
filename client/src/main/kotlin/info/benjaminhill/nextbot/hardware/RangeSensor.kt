package info.benjaminhill.nextbot.hardware

import com.google.firebase.database.Exclude
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinPullResistance
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/** HC-SR04 Ultrasonic Distance Sensor */
open class RangeSensor : AutoCloseable, CoroutineScope {
    override fun close() {
        gpio.shutdown()
    }

    @Transient
    private var job = Job()

    @get:Exclude
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    // https://gist.github.com/rutgerclaes/6713672
    // TODO: Do we need https://medium.com/@chandima/a-java-class-to-read-ultrasonic-sensor-readings-raspberry-pi-hc-sr04-8cd29123dab1
    // TODO: or even faster https://groups.google.com/forum/#!msg/pi4j/XoZXD2VxG1M/IsJAJnu3BgAJ

    private val sensorTriggerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00)!! // Trigger pin as OUTPUT
    private val sensorEchoPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN)!! // Echo pin as INPUT

    init {
        sensorTriggerPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
        sensorEchoPin.addListener(GpioPinListenerDigital { event -> println("${System.nanoTime()}: ${event.pin} = ${event.state}") })
    }

    // TODO: Always b pinging. https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/EvictingQueue.html
    suspend fun ping(): Double {
        delay(Duration.ofSeconds(1).toMillis())
        sensorTriggerPin.high()
        delay(TimeUnit.MICROSECONDS.toMillis(10))
        sensorTriggerPin.low()

        while (sensorEchoPin.isLow) {
            // spin
        }
        val startTime = System.nanoTime()
        while (sensorEchoPin.isHigh) {
            // spin
        }
        val endTime = System.nanoTime()
        val cm = (((endTime - startTime) / 1e3) / 2) / 29.1
        return cm
    }

    companion object {
        private val gpio = GpioFactory.getInstance()!!
    }
}