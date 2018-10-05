package client

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import info.benjaminhill.nextbot.CloudBot
import info.benjaminhill.nextbot.TwoWheelBot
import kotlinx.coroutines.delay


import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger


fun main() = runBlocking {
    LogManager.getLogManager().getLogger("").apply { level = Level.INFO }.handlers.forEach { it.level = Level.INFO }
    val log = Logger.getLogger(CloudBot::class.java.name)!!

    log.info("All is good.")

    val fbIdKey = Key("fb.id", stringType)
    val uIdKey = Key("user.id", stringType)
    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromFile(File("resources/nextbot.properties"))

    TwoWheelBot(config[fbIdKey], config[uIdKey]).use { twb ->
        twb.open()
        delay(3, TimeUnit.SECONDS)
        twb.setLeftSpeed(.5)
        twb.setRightSpeed(-1.0)
        delay(10, TimeUnit.SECONDS)
    }
}

