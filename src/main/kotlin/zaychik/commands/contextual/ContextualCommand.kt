package zaychik.commands.contextual

import dev.kord.core.event.Event

interface ContextualCommand<T: Event> {
    suspend fun check(event: T): Boolean
    suspend fun action(event: T)
}