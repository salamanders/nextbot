package info.benjaminhill.nextbot.hardware

import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import java.util.logging.Logger

/**
 * Minimal I2C Driver for the Picon Zero Motor Hat https://4tronix.co.uk/blog/?p=1224
 */
class PiconZero {
    private var log: Logger = Logger.getLogger(this::class.java.name)!!

    enum class Motor(val id: Int) {
        MOTOR_0(0),
        MOTOR_1(1)
    }

    private val device: I2CDevice

    init {
        device = try {
            // Why BUS_1?  Dunno.
            I2CFactory.getInstance(I2CBus.BUS_1).getDevice(PICON_ZERO_ADDRESS)
        } catch (e: I2CFactory.UnsupportedBusNumberException) {
            FakeI2CDevice("fakeI2Cpz")
        }
        device.write(REGISTER_RESET, 0.toByte())
    }

    /**
     * @param motor 0 or 1
     * @param speed motor 0: -1 to 1 inclusive
     */
    fun setDCMotor(motor: Motor, speed: Double) {
        require(speed in -1.0..1.0)
        val intSpeed = ((speed + 1.0) / 2 * (127 + 128) - 128).toInt()
        log.fine("setting DC motor ${motor.id} to $intSpeed")
        device.write(motor.id, intSpeed.toByte())
    }

    companion object {
        private const val REGISTER_RESET = 20
        private const val PICON_ZERO_ADDRESS = 0x22
    }
}