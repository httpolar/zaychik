package zaychik.commands.app

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.modify.actionRow
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.commands.abstracts.AppCommand
import zaychik.db.entities.ReactRole
import zaychik.db.tables.ReactRolesTable
import zaychik.extensions.waitFor
import zaychik.utils.partialEmoji


class DeleteReactRolesAppCommand : AppCommand() {
    override val name = "Delete React Roles"

    override suspend fun check(event: GuildMessageCommandInteractionCreateEvent): Boolean {
        return event.interaction.user.asMember().getPermissions().contains(Permission.ManageRoles)
    }

    override suspend fun action(event: GuildMessageCommandInteractionCreateEvent) {
        val kord = event.kord
        val guild = event.interaction.guild
        val user = event.interaction.user
        val srcMessage = event.interaction.target

        val fetchingDataResponse = event.interaction.deferEphemeralResponse()

        val reactRoleEntries = newSuspendedTransaction(Dispatchers.IO) {
            ReactRole.find { ReactRolesTable.messageId eq event.interaction.target.id.value }.limit(25).toList()
        }

        if (reactRoleEntries.isEmpty()) {
            fetchingDataResponse.respond {
                content = "This message doesn't have any react roles associated with it!"
            }
            return
        }

        val reactRoles = reactRoleEntries.map { guild.getRoleOrNull(Snowflake(it.roleId)) }
        val reactRoleEntriesAndRoles = reactRoleEntries.zip(reactRoles)

        val reactRoleSelectId = UUID.randomUUID().toString()
        val selectMenuResponse = fetchingDataResponse.respond {
            content = "React roles associated with the selected message:"

            actionRow {
                stringSelect(reactRoleSelectId) {
                    allowedValues = 1..reactRoleEntries.size

                    reactRoleEntriesAndRoles.forEach { (entry, role) ->
                        option("@${role?.name}", entry.id.toString()) {
                            emoji = partialEmoji(entry.emojiId, isAnimated = entry.isEmojiAnimated)
                        }
                    }
                }
            }
        }

        val selectMenuResult = kord.waitFor<GuildSelectMenuInteractionCreateEvent> {
            it.interaction.user.id == user.id && it.interaction.component.customId == reactRoleSelectId
        }

        val selectMenuEvent = selectMenuResult.getOrNull()

        if (selectMenuEvent == null) {
            selectMenuResponse.edit {
                content = when (selectMenuResult.exceptionOrNull()) {
                    is TimeoutCancellationException -> ":warning: Timed out! Please, try again."
                    else -> ":warning: Something went wrong! Please, try again."
                }
                components = mutableListOf()
            }
            return
        }

        val selectedReactRoleIds = selectMenuEvent.interaction.values
        val selectedReactRoles = reactRoleEntriesAndRoles.filter { (entry, _) ->
            selectedReactRoleIds.contains("${entry.id}")
        }

        val reactionEmojisToRemove = srcMessage.asMessageOrNull()?.reactions
            ?.filter { reaction ->
                val emoji = reaction.emoji as? ReactionEmoji.Custom ?: return@filter false

                selectedReactRoles.any { (reactRole, _) ->
                    emoji.id.value == reactRole.emojiId && reaction.selfReacted
                }
            }
            ?.map { it.emoji as ReactionEmoji.Custom }

        if (reactionEmojisToRemove == null) {
            selectMenuResponse.edit {
                content = "There are no react roles that could be removed from the original message!"
                components = mutableListOf()
            }
            return
        }

        newSuspendedTransaction(Dispatchers.IO) {
            ReactRolesTable.deleteWhere { this.emojiId inList reactionEmojisToRemove.map { it.id.value } }
        }

        reactionEmojisToRemove.forEach {
            srcMessage.deleteOwnReaction(it)
        }

        selectMenuResponse.edit {
            content = "Done! The previously selected react roles have been deleted."
            components = mutableListOf()
        }
    }
}