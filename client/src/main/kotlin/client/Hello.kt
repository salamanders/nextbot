package client


import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import info.benjaminhill.nextbot.TwoWheelBot
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
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
            ConfigurationProperties.fromResource("nextbot.properties")

    //val rs = RangeSensor()
    //println("Ping: ${rs.ping()}")
    //System.exit(-1)

    TwoWheelBot(config[fbIdKey], config[uIdKey]).use { twb ->
        twb.initObserversAndSync()
        twb.script = javaClass.getResource("/script.default.js").readText()
        delay(Duration.ofSeconds(1).toMillis())
        twb.setLeftSpeed(.95)
        twb.setRightSpeed(-1.0)
        delay(Duration.ofSeconds(15).toMillis())
    }

    logger.info { "Spinning down!" }
}

