package zaychik.commands.app

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.rest.builder.message.modify.actionRow
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.commands.abstracts.AppCommand
import zaychik.db.entities.ReactRole
import zaychik.extensions.waitFor


class CreateReactRoleAppCommand : AppCommand() {
    override val name = "Create React Role"
    private val timeout = 90.seconds

    override suspend fun check(event: GuildMessageCommandInteractionCreateEvent): Boolean {
        return event.interaction.user.asMember().getPermissions().values.contains(Permission.ManageRoles)
    }

    override suspend fun action(event: GuildMessageCommandInteractionCreateEvent) {
        val kord = event.kord
        val actor = event.interaction.user
        val guild = event.interaction.guild
        val srcMessage = event.interaction.target

        val deferredResponse = event.interaction.deferEphemeralResponse()

        val roleSelectId = UUID.randomUUID().toString()
        val roleSelectResponse = deferredResponse.respond {
            content = "Please, select the role that we're going to be giving out."
            actionRow {
                roleSelect(customId = roleSelectId)
            }
        }

        val roleSelectResult = kord.waitFor<GuildSelectMenuInteractionCreateEvent>(timeout) {
            it.interaction.user.id == actor.id && it.interaction.component.customId == roleSelectId
        }

        val roleSelectEvent = roleSelectResult.getOrNull()

        if (roleSelectEvent == null) {
            val errorMessage = when (roleSelectResult.exceptionOrNull()) {
                is TimeoutCancellationException -> ":warning: Timed out! It took too long for you to select the role, please, retry."
                else -> ":warning: Something went wrong, please, try again."
            }

            roleSelectResponse.createEphemeralFollowup { content = errorMessage }
            roleSelectResponse.delete()
            return
        }

        val deferredSelectResponse = roleSelectEvent.interaction.deferEphemeralResponse()
        val role = roleSelectEvent.interaction.resolvedObjects?.roles?.values?.first()
        if (role == null) {
            deferredSelectResponse.respond {
                content = ":warning: The role you've selected was not present " +
                        "in event's resolved object, please, try again."
            }
            return
        }


        val reactionRequestResponse = deferredSelectResponse.respond {
            content = "You've picked the ${role.mention} role, now you'll have to add a reaction which would " +
                    "essentially be giving or taking the role from users who click on the reaction."
        }

        val reactionResult = kord.waitFor<ReactionAddEvent>(timeout) { e ->
            val isValidMessage = e.message.id == srcMessage.id
            val isValidUser = e.user.id == actor.id
            val isValidEmoji = guild.emojis.firstOrNull { it.id == (e.emoji as? ReactionEmoji.Custom)?.id } != null

            isValidMessage && isValidUser && isValidEmoji
        }

        val reactionEvent = reactionResult.getOrNull()
        if (reactionEvent == null) {
            val errorMessage = when (reactionResult.exceptionOrNull()) {
                is TimeoutCancellationException -> ":warning: Timed out! It took too long for you to add a reaction, please, retry."
                else -> ":warning: Something went wrong, please, try again."
            }

            reactionRequestResponse.edit { content = errorMessage }
            roleSelectResponse.delete()
            return
        }

        // no need to do this safely since we check in the filter of waitFor
        val reactionEmoji = reactionEvent.emoji as ReactionEmoji.Custom

        srcMessage.addReaction(reactionEmoji)

        newSuspendedTransaction(Dispatchers.IO) {
            ReactRole.new {
                this.guildId = guild.id.value
                this.channelId = srcMessage.channelId.value
                this.messageId = srcMessage.id.value
                this.roleId = role.id.value
                this.emojiId = reactionEmoji.id.value
                this.isEmojiAnimated = reactionEmoji.isAnimated
                this.enabled = true
            }
        }

        reactionRequestResponse.createEphemeralFollowup {
            content = ":white_check_mark: You have successfully created a reaction role for ${role.mention}"
        }
    }
}
