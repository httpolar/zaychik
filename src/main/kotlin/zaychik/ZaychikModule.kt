package zaychik

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.koin.dsl.module

val zaychikModule = module {
    single {
        Config("config.properties")
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
        Zaychik()
    }
}
