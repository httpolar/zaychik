package zaychik.commands

import dev.kord.core.event.Event
import kotlin.properties.Delegates


class ExecutableCommand<T : Event>(private val event: T) {
    private var command: Command<T> by Delegates.notNull()
    private var onCheckFailure: suspend (T) -> Unit = {}

    fun command(cmd: Command<T>): ExecutableCommand<T> {
        command = cmd
        return this
    }

    fun onCheckFailure(block: suspend (T) -> Unit): ExecutableCommand<T> {
        onCheckFailure = block
        return this
    }

    suspend fun execute() {
        val canRun = command.check(event)
        if (!canRun) {
            onCheckFailure.invoke(event)
        }
        command.action(event)
    }
}

fun <T : Event> executableCommand(event: T): ExecutableCommand<T> {
    return ExecutableCommand(event)
}