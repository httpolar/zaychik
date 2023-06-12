package zaychik.commands.slash

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import zaychik.commands.abstracts.SlashCommand

class CatSlashCommand : SlashCommand() {
    override val name = "cat"
    override val description = "Provides you with fluffy cats"

    override suspend fun check(event: GuildChatInputCommandInteractionCreateEvent): Boolean {
        return true
    }

    override suspend fun action(event: GuildChatInputCommandInteractionCreateEvent) {
        event.interaction.respondPublic {
            embed {
                image = "https://cataas.com/cat"
                title = "Here's a cat"
            }
        }
    }
}