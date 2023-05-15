package zaychik.commands.abstracts

import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent

abstract class AppCommand : Command<GuildMessageCommandInteractionCreateEvent>() {
    abstract val name: String
}