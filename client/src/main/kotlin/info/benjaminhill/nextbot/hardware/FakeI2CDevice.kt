package info.benjaminhill.nextbot.hardware

import com.pi4j.io.i2c.I2CDevice
import info.benjaminhill.nextbot.CloudBot
import mu.KLoggable
import java.nio.ByteBuffer
import java.nio.IntBuffer

class FakeI2CDevice(val name: String) : I2CDevice {

    private var lastValue = mutableMapOf<Any, Any?>()
    private fun writeAny(key: String, value: Any?) {
        if (lastValue.put(key, value) != value) {
            logger.info { "$key = $value" }
        }
    }

    init {
        logger.info { "FakeI2CDevice($name)" }
    }

    override fun getAddress(): Int = 0

    override fun write(p0: ByteArray?) {
        writeAny("write.array()", p0)
    }

    override fun write(p0: Int, p1: ByteArray?) {
        writeAny("write.array($p0)", p1)
    }

    override fun write(p0: ByteArray?, p1: Int, p2: Int) {
        writeAny("write.array($p1:$p2)", p0)
    }

    override fun write(p0: Int, p1: ByteArray?, p2: Int, p3: Int) {
        writeAny("write.array($p0:$p2:$p3)", p1)
    }

    override fun write(p0: Byte) {
        writeAny("write.byte", p0)
    }

    override fun write(p0: Int, p1: Byte) {
        writeAny("write.byte($p0)", p1)
    }

    override fun ioctl(p0: Long, p1: Int) {
        logger.info { "$name ioctl:$p0 $p1" }
    }

    override fun ioctl(p0: Long, p1: ByteBuffer?, p2: IntBuffer?) {
        logger.info { "$name ioctl:$p0 $p1 $p2" }
    }

    override fun read(): Int = 0

    override fun read(p0: ByteArray?, p1: Int, p2: Int): Int = 0

    override fun read(p0: Int): Int = 0

    override fun read(p0: Int, p1: ByteArray?, p2: Int, p3: Int): Int = 0

    override fun read(p0: ByteArray?, p1: Int, p2: Int, p3: ByteArray?, p4: Int, p5: Int): Int = 0

    companion object : KLoggable {
        override val logger = CloudBot.logger()
    }

}