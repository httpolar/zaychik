package zaychik.commands.abstracts

import dev.kord.core.event.Event
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.OptionsBuilder
import kotlin.reflect.full.createInstance

abstract class Command<T : Event> {
    abstract suspend fun check(event: T): Boolean
    abstract suspend fun action(event: T)
}

abstract class AppCommand : Command<GuildMessageCommandInteractionCreateEvent>() {
    abstract val name: String
}

abstract class SlashCommand : Command<GuildChatInputCommandInteractionCreateEvent>() {
    open val rootName: String? = null
    abstract val name: String
    abstract val description: String
    open val options: MutableList<OptionsBuilder>? = null

    val fullName by lazy {
        if (rootName != null) "$rootName $name" else name
    }
}

inline fun <reified T : AppCommand> appCommand(): Pair<String, T> {
    val instance = T::class.createInstance()
    return instance.name to instance
}

inline fun <reified C : SlashCommand> slashCommand(): Pair<String, C> {
    val instance = C::class.createInstance()
    return instance.fullName to instance
}
