package info.benjaminhill.nextbot

import com.pi4j.io.i2c.I2CDevice
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.logging.Logger

class FakeI2CDevice(val name:String) : I2CDevice {
    private var log: Logger = Logger.getLogger(this::class.java.name)!!

    init {
        log.warning("FakeI2CDevice($name)")
    }

    override fun getAddress(): Int = 0

    override fun write(p0: Byte) {
        log.warning("$name write:$p0")
    }

    override fun write(p0: ByteArray?, p1: Int, p2: Int) {
        log.warning("$name write:$p0 $p1 $p2")
    }

    override fun write(p0: ByteArray?) {
        log.warning("$name write:$p0")
    }

    override fun write(p0: Int, p1: Byte) {
        log.warning("$name write:$p0 $p1")
    }

    override fun write(p0: Int, p1: ByteArray?, p2: Int, p3: Int) {
        log.warning("$name write:$p0 $p1 $p2 $p3")
    }

    override fun write(p0: Int, p1: ByteArray?) {
        log.warning("$name write:$p0 $p1")
    }

    override fun ioctl(p0: Long, p1: Int) {
        log.warning("$name ioctl:$p0 $p1")
    }

    override fun ioctl(p0: Long, p1: ByteBuffer?, p2: IntBuffer?) {
        log.warning("$name ioctl:$p0 $p1 $p2")
    }

    override fun read(): Int = 0

    override fun read(p0: ByteArray?, p1: Int, p2: Int): Int = 0

    override fun read(p0: Int): Int = 0

    override fun read(p0: Int, p1: ByteArray?, p2: Int, p3: Int): Int = 0

    override fun read(p0: ByteArray?, p1: Int, p2: Int, p3: ByteArray?, p4: Int, p5: Int): Int = 0

}