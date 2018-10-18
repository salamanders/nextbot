package info.benjaminhill.nextbot

import mu.KLoggable
import kotlin.reflect.KMutableProperty

/** Handles inverted forwards/backwards and left/right */
open class TankBot(uId: String) : PiconZeroBot(uId) {

    var leftSpeed by RunnableCloudSyncDelegate(0.0)
    var rightSpeed by RunnableCloudSyncDelegate(0.0)

    var isMotor0Right by RunnableCloudSyncDelegate(true)
    var isRightForward by RunnableCloudSyncDelegate(true)
    var isLeftForward by RunnableCloudSyncDelegate(true)

    /** Bi-directional cross linking of sliders.
     * You set them, they try to set you back, but your value hasn't changed so the loop stops.
     */
    private fun toAssociatedSlider(slider: KMutableProperty<Double>): KMutableProperty<Double> = when (slider) {
        this::rightSpeed -> if (isMotor0Right) this::motor0 else this::motor1
        this::leftSpeed -> if (isMotor0Right) this::motor1 else this::motor0
        this::motor0 -> if (isMotor0Right) this::rightSpeed else this::leftSpeed
        this::motor1 -> if (isMotor0Right) this::leftSpeed else this::rightSpeed
        else -> throw IllegalArgumentException("No toAssociatedSlider mapping for ${slider.name}")
    }

    /** Which links get flipped */
    private fun toAssociatedFlip(slider: KMutableProperty<Double>): Int = when (slider) {
        this::rightSpeed -> if (isRightForward) 1 else -1
        this::leftSpeed -> if (isLeftForward) 1 else -1
        this::motor0 -> if (isMotor0Right) toAssociatedFlip(this::rightSpeed) else toAssociatedFlip(this::leftSpeed)
        this::motor1 -> if (isMotor0Right) toAssociatedFlip(this::leftSpeed) else toAssociatedFlip(this::rightSpeed)
        else -> throw IllegalArgumentException("No toAssociatedFlip mapping for ${slider.name}")
    }

    init {
        setOf(this::leftSpeed, this::rightSpeed, this::motor0, this::motor1).forEach { slider ->
            addObserver(slider) {
                logger.info { "Slider cross-linkage: ${slider.name}=${slider.get()}, so ${toAssociatedSlider(slider).name}=${slider.get() * toAssociatedFlip(slider)}" }
                toAssociatedSlider(slider).setter.call(slider.get() * toAssociatedFlip(slider))
            }
        }
    }

    companion object : KLoggable {
        override val logger = logger()
    }
}