package zaychik.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CompletableDeferred
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.Config
import kotlin.properties.Delegates

object ZaychikDatabase {
    private val logger = KotlinLogging.logger {}

    private var database by Delegates.notNull<Database>()
    private val readyDeferred = CompletableDeferred<Unit>()

    @JvmStatic
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = Config.Hikari.jdbcUrl()
        username = Config.Hikari.user
        password = Config.Hikari.password
        maximumPoolSize = 10
    }

    @JvmStatic
    private val hikariSource = HikariDataSource(hikariConfig)

    @JvmStatic
    private suspend fun createConnection() {
        database = Database.connect(hikariSource)

        newSuspendedTransaction {
            exec("SELECT 1;")
            readyDeferred.complete(Unit)
            logger.info { "Database connection created!" }
        }
    }

    @JvmStatic
    suspend fun connect() {
        createConnection()
        readyDeferred.await()
    }
}
