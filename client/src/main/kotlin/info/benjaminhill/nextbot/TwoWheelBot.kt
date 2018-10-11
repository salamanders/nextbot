package info.benjaminhill.nextbot

import info.benjaminhill.nextbot.hardware.PiconZero
import mu.KLoggable


open class TwoWheelBot(fbId: String, uId: String) : ScriptableCloudBot(fbId, uId) {

    @Transient
    private val motorShield = PiconZero()

    private var motor0 by syncToCloud(0.0)
    private var motor1 by syncToCloud(0.0)

    var leftSpeed by syncToCloud(0.0)
    var rightSpeed by syncToCloud(0.0)

    var isMotor0Right by syncToCloud(false)
    var isRightForward by syncToCloud(false)
    var isLeftForward by syncToCloud(false)

    override fun initObserversAndSync() {
        super.initObserversAndSync()
        observers[this::motor0.name]!!.add { motorShield.setDCMotor(0, motor0) }
        observers[this::motor1.name]!!.add { motorShield.setDCMotor(1, motor1) }

        observers[this::leftSpeed.name]!!.add {
            if (isMotor0Right) {
                motor0 = leftSpeed * if (isRightForward) 1 else -1
            } else {
                motor1 = leftSpeed * if (isRightForward) 1 else -1
            }
        }

        observers[this::rightSpeed.name]!!.add {
            if (isMotor0Right) {
                motor1 = rightSpeed * if (isLeftForward) 1 else -1
            } else {
                motor0 = rightSpeed * if (isLeftForward) 1 else -1
            }
        }

        observers[this::running.name]!!.add {
            if (!running) {
                logger.info { "Not running, shutting down motors." }
                motor0 = 0.0
                motor1 = 0.0
            }
        }

        logger.info { "TwoWheelBot Observer Keys (and count):" + observers.entries.joinToString(", ") { "${it.key} (${it.value.size})" } }
    }

    companion object : KLoggable {
        override val logger = CloudBot.logger()
    }
}