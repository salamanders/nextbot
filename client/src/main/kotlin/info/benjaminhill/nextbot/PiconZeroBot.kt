package info.benjaminhill.nextbot

import info.benjaminhill.nextbot.cloud.ScriptableCloudBot
import info.benjaminhill.nextbot.hardware.PiconZero
import mu.KLoggable

/** PiconZero drives two higher voltage DC motors */
open class PiconZeroBot(uId: String) : ScriptableCloudBot(uId) {

    @Transient
    private val motorShield = PiconZero()

    var motor0: Double by RunnableCloudSyncDelegate(
            initialValue = 0.0,
            shutdownValue = 0.0
    )
    var motor1: Double by RunnableCloudSyncDelegate(
            initialValue = 0.0,
            shutdownValue = 0.0
    )

    init {
        addObserver(this::motor0) {
            motorShield.setDCMotor(PiconZero.Motor.MOTOR_0, motor0)
        }

        addObserver(this::motor1) {
            motorShield.setDCMotor(PiconZero.Motor.MOTOR_1, motor1)
        }
    }

    companion object : KLoggable {
        override val logger = logger()
    }
}