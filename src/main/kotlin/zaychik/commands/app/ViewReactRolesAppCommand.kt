package zaychik.commands.app

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.commands.abstracts.AppCommand
import zaychik.db.entities.ReactRole
import zaychik.db.tables.ReactRolesTable

class ViewReactRolesAppCommand : AppCommand() {
    override val name = "View React Roles"

    override suspend fun check(event: GuildMessageCommandInteractionCreateEvent): Boolean {
        return event.interaction.user.asMember().getPermissions().contains(Permission.ManageRoles)
    }

    override suspend fun action(event: GuildMessageCommandInteractionCreateEvent) {
        val message = event.interaction.target

        val reactRoles = newSuspendedTransaction(Dispatchers.IO) {
            ReactRole.find { ReactRolesTable.messageId eq message.id.value }.toList()
        }

        if (reactRoles.isEmpty()) {
            event.interaction.respondEphemeral {
                content = ":x: This message has no reaction roles attached to it."
            }
        }

        val emojis = message.asMessage().reactions.toList()

        val responseContent = StringBuilder()
        reactRoles.forEach {
            val uuid = "`${it.id}`"
            val role = "<@&${it.roleId}>"

            val emoji = emojis
                .firstOrNull { r -> r.id?.value == it.emojiId }
                ?.emoji
                ?.mention

            responseContent.appendLine("$uuid | $emoji -> $role")
        }

        event.interaction.respondEphemeral {
            embed {
                title = "List of reaction roles"
                description = responseContent.toString()
            }
        }
    }
}