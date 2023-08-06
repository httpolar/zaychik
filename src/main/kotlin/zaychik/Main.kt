package zaychik

import dev.kord.core.Kord
import org.koin.core.context.startKoin


suspend fun main() {
    val koin = startKoin {
        modules(zaychikModule)
    }.koin

    val cfg = koin.get<Config>()

    val kord = Kord(cfg.token)
    koin.declare(kord)

    koin.get<Zaychik>()
        .start()
}
