package zaychik.commands.abstracts

import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import kotlin.reflect.full.createInstance

abstract class AppCommand : Command<GuildMessageCommandInteractionCreateEvent>() {
    abstract val name: String
}

inline fun <reified T : AppCommand> appCommand(): Pair<String, T> {
    val instance = T::class.createInstance()
    return instance.name to instance
}
