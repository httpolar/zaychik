package zaychik.commands

import dev.kord.core.event.Event

abstract class Command<T : Event> {
    abstract suspend fun check(event: T): Boolean
    abstract suspend fun action(event: T)

    suspend fun checkAndRun(event: T, onCheckFailure: suspend (T) -> Unit = {}) {
        if (check(event)) {
            action(event)
        } else {
            onCheckFailure(event)
        }
    }
}

