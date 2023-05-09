package zaychik.commands.app

import dev.kord.core.event.Event

abstract class ApplicationCommand<T : Event> {
    abstract suspend fun check(event: T): Boolean
    abstract suspend fun action(event: T)
}

