package zaychik

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val zaychikModule = module {
    single { Config("config.properties") } withOptions {
        createdAtStart()
    }

    single {
        val zaychikConfig = get<Config>()
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = zaychikConfig.jdbcUrl
            driverClassName = zaychikConfig.driverClass
            maximumPoolSize = 10
        }
        HikariDataSource(hikariConfig)
    }

    single {
        val config = get<Config>()
        runBlocking {
            Kord(config.token)
        }
    }

    single {
        Zaychik()
    }
}
