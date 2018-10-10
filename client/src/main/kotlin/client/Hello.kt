package client


import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import info.benjaminhill.nextbot.TwoWheelBot
import info.benjaminhill.nextbot.hardware.RangeSensor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File
import java.time.Duration
import java.util.logging.Level
import java.util.logging.LogManager

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    LogManager.getLogManager().getLogger("").apply { level = Level.WARNING }.handlers.forEach { it.level = Level.WARNING }

    logger.info { "Spinning up!" }

    val fbIdKey = Key("fb.id", stringType)
    val uIdKey = Key("user.id", stringType)
    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromFile(File("resources/nextbot.properties"))


    val rs = RangeSensor()
    println("Ping: ${rs.ping()}")
    System.exit(-1)

    TwoWheelBot(config[fbIdKey], config[uIdKey]).use { twb ->
        twb.open()
        twb.script = "result.motor0 = bot.motor0 * 0.9; let x = notreal.obj;"
        delay(Duration.ofSeconds(1).toMillis())
        twb.setLeftSpeed(.95)
        twb.setRightSpeed(-1.0)
        delay(Duration.ofSeconds(15).toMillis())
    }

    logger.info { "Spinning down!" }
}

