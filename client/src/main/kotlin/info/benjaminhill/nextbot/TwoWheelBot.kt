package info.benjaminhill.nextbot

import java.util.logging.Logger

open class TwoWheelBot(fbId: String, uId:String) : CloudBot(fbId, uId) {
    @Transient
    private var log: Logger = Logger.getLogger(this::class.java.name)!!

    @Transient
    private val motorShield = PiconZero()

    init {
        observers[this::motor0.name]!!.add { motorShield.setDCMotor(0, motor0) }
        observers[this::motor1.name]!!.add { motorShield.setDCMotor(1, motor1) }
        observers[this::active.name]!!.add { if(!active) {
            log.info("Not active, shutting down motors.")
            motor0 = 0.0
            motor1 = 0.0
        }}

        println("TwoWheelBot Observers:" + observers.entries.joinToString(", ") { "${it.key}=${it.value.size}" })
    }

    fun setLeftSpeed(speed: Double) {
        require(speed in -1.0..1.0)
        if (isMotor0Right) {
            motor0 = speed * if (isRightForward) 1 else -1
        } else {
            motor1 = speed * if (isRightForward) 1 else -1
        }
    }

    fun setRightSpeed(speed: Double) {
        require(speed in -1.0..1.0)
        if (isMotor0Right) {
            motor1 = speed * if (isLeftForward) 1 else -1
        } else {
            motor0 = speed * if (isLeftForward) 1 else -1
        }
    }

    var motor0 by syncToCloud(0.0)
    var motor1 by syncToCloud(0.0)
    var isMotor0Right by syncToCloud(false)
    var isRightForward by syncToCloud(false)
    var isLeftForward by syncToCloud(false)
}