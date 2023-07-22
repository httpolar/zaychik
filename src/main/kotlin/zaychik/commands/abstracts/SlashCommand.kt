package zaychik.commands.abstracts

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.OptionsBuilder
import kotlin.reflect.full.createInstance


abstract class SlashCommand : Command<GuildChatInputCommandInteractionCreateEvent>() {
    open val rootName: String? = null
    abstract val name: String
    abstract val description: String
    open val options: MutableList<OptionsBuilder>? = null

    val fullName by lazy {
        if (rootName != null) "$rootName $name" else name
    }
}

inline fun <reified C : SlashCommand> slashCommand(): Pair<String, C> {
    val instance = C::class.createInstance()
    return instance.fullName to instance
}