package zaychik

import dev.kord.core.Kord
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val logger = KotlinLogging.logger {}


fun zaychikModule() = module {
    singleOf(::kordFactory)
    singleOf(::Zaychik)
}

fun kordFactory() = runBlocking {
    logger.info { "Created an instance of Kord" }
    return@runBlocking Kord(Config.Bot.token)
}

suspend fun main() {
    Config.load("config.properties")  // This will use config.properties of the working directory

    val koin = startKoin {
        modules(zaychikModule())
    }.koin

    koin.get<Zaychik>().start()
}