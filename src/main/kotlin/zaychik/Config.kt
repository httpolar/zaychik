package zaychik

import mu.KotlinLogging
import java.io.File
import java.util.Properties
import kotlin.system.exitProcess

object Config {
    private val logger = KotlinLogging.logger {}

    private val properties = Properties().apply {
        setProperty("bot.token", "insert your token here")
        setProperty("bot.guild", "12312345151")
        setProperty("hikari.jdbc", "jdbc:sqlite:storage.db")
        setProperty("hikari.driverClass", "org.sqlite.JDBC")
    }

    object Bot {
        val token: String = properties.getProperty("bot.token")
        val guild: String = properties.getProperty("bot.guild")
    }

    object Hikari {
        val jdbcUrl: String = properties.getProperty("hikari.jdbc")
        val driverClass: String = properties.getProperty("hikari.driverClass")
    }

    fun load(path: String = "config.properties") {
        val f = File(path)
        if (f.exists()) {
            properties.load(f.reader())
        }

        if (!f.exists()) {
            f.writer().use {
                properties.store(it, "Project Zaychik 0019")
                it.close()

                logger.info { "Configuration file did not exist, I have pre-generated a new one for you!" }
                exitProcess(1)
            }

        }
    }
}