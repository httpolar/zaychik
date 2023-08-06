package zaychik

import org.koin.core.context.startKoin


suspend fun main() {
    val koin = startKoin {
        modules(zaychikModule)
    }.koin

    koin.get<Zaychik>()
        .start()
}