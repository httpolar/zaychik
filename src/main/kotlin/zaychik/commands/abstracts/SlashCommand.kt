package zaychik.commands.abstracts

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.OptionsBuilder


abstract class SlashCommand : Command<GuildChatInputCommandInteractionCreateEvent>() {
    abstract val rootName: String?
    abstract val name: String
    abstract val description: String
    abstract val options: MutableList<OptionsBuilder>?

    val fullName by lazy {
        if (rootName != null) "$rootName $name" else name
    }
}
