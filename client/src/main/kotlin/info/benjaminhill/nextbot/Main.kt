package info.benjaminhill.nextbot

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    logger.info { "Spinning up!" }
    val uIdKey = Key("user.id", stringType)
    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("nextbot.properties")

    TankBot(config[uIdKey]).use { bot ->
        // logger.info { "Observers:" + bot.observers.entries.joinToString(", ") { "${it.key} (${it.value.size})" } }
        println("Starting and chilling...")
        bot.startSync()

        delay(Duration.ofSeconds(3).toMillis())

        println("Zeroing out motors.")
        bot.motor0 = 0.0
        bot.motor1 = 0.0
        delay(Duration.ofSeconds(2).toMillis())

        println("Setting motor0 to half power")
        bot.motor0 = 0.5
        delay(Duration.ofSeconds(5).toMillis())

        println("Setting right (which should be motor0) to full power")
        bot.rightSpeed = 1.0
        delay(Duration.ofSeconds(5).toMillis())

        println("Shutting down robot.")
    }
}
