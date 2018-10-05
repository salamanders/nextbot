package info.benjaminhill.nextbot

import com.pi4j.io.i2c.I2CBus
import com.pi4j.io.i2c.I2CDevice
import com.pi4j.io.i2c.I2CFactory
import java.util.logging.Logger

/**
 * Minimal I2C Driver for the Picon Zero Motor Hat https://4tronix.co.uk/blog/?p=1224
 */
class PiconZero {
    private var log: Logger = Logger.getLogger(this::class.java.name)!!
    private val device: I2CDevice

    init {
        device = try {
            I2CFactory.getInstance(I2CBus.BUS_0).getDevice(PICON_ZERO_ADDRESS)
        } catch (e: I2CFactory.UnsupportedBusNumberException) {
            FakeI2CDevice("fakePiconZero")
        }
        device.write(REGISTER_RESET, 0.toByte())
    }

    /**
     * @param motorId 0 or 1
     * @param speed motor 0: -1 to 1 inclusive
     */
    fun setDCMotor(motorId: Int, speed: Double) {
        require(speed in -1.0..1.0)
        require(motorId in 0..1)
        val intSpeed = ((speed + 1.0) / 2 * (127 + 128) - 128).toInt()
        log.info("setting DC motor $motorId to $intSpeed")
        device.write(motorId, intSpeed.toByte())
    }

    companion object {
        private const val REGISTER_RESET = 20
        private const val PICON_ZERO_ADDRESS = 0x22
    }
}