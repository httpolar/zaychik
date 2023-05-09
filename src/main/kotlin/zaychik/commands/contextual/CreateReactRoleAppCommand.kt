package zaychik.commands.contextual

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.db.entities.ReactRole
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds

class CreateReactRoleAppCommand : ApplicationCommand<GuildMessageCommandInteractionCreateEvent>() {
    companion object {
        const val name = "Create React Role"
    }

    override suspend fun check(event: GuildMessageCommandInteractionCreateEvent): Boolean {
        return event.interaction.user.asMember().getPermissions().values.contains(Permission.ManageRoles)
    }

    @OptIn(FlowPreview::class)
    override suspend fun action(event: GuildMessageCommandInteractionCreateEvent) {
        val kord = event.kord
        val actor = event.interaction.user
        val guild = event.interaction.guild
        val srcMessage = event.interaction.target

        val botsHighestRole = event.interaction.guild
            .getMember(event.kord.selfId)
            .roles
            .toCollection(ArrayList())
            .maxByOrNull { it.rawPosition }

        if (botsHighestRole == null) {
            event.interaction.respondEphemeral {
                content =
                    "I do not have enough permissions to perform this action! Please, give me a higher role on your server, so I could give roles to people. pls."
            }
            return
        }

        val roleSelectId = UUID.randomUUID().toString()

        event.interaction.respondEphemeral {
            content = "Please, select the role that we're going to be giving out."
            actionRow {
                roleSelect(customId = roleSelectId)
            }
        }


        val selectMenuEvent = kord.events.buffer(Channel.UNLIMITED)
            .filterIsInstance<GuildSelectMenuInteractionCreateEvent?>()
            .filter { e -> e?.interaction?.user?.id == actor.id }
            .take(1)
            .timeout(90.seconds)
            .catch { emit(null) }
            .firstOrNull()
            ?: // tell user something fucked up
            return

        val role = selectMenuEvent.interaction.resolvedObjects
            ?.roles
            ?.values
            ?.firstOrNull()

        if (role == null) {
            event.interaction.respondEphemeral {
                content = ":x: Well, something went wrong and the role you've selected have not been resolved..."
            }
            return
        }

        val askingUserForReactionInteraction = selectMenuEvent.interaction.updateEphemeralMessage {
            content = "Please, add a reaction to the original message and then this reaction will be used as a trigger."
        }

        val reactionEmoji = kord.events.buffer(Channel.UNLIMITED)
            .filterIsInstance<ReactionAddEvent?>()
            .filter { r -> r?.user?.id == actor.id && r.messageId == srcMessage.id }
            .take(1)
            .timeout(90.seconds)
            .catch { emit(null) }
            .firstOrNull()
            ?.emoji

        if (reactionEmoji !is ReactionEmoji.Custom) {
            askingUserForReactionInteraction.edit {
                content = ":x: You must provide a custom emoji, unicode emojis are not supported. Please, start again."
            }
            return
        }

        val emoji = guild.emojis.firstOrNull { e -> e.id == reactionEmoji.id }
        if (emoji == null) {
            askingUserForReactionInteraction.edit {
                content = ":x: You must use an emoji that belongs to this server. Please, start again."
            }
            return
        }


        try {
            srcMessage.addReaction(emoji)
        } catch (e: RestRequestException) {
            askingUserForReactionInteraction.edit {
                content = ":x: I couldn't process the emoji you have used as a reaction. Please, start again."
            }
            return
        }

        newSuspendedTransaction(Dispatchers.IO) {
            ReactRole.new {
                this.guildId = guild.id.value.toLong()
                this.channelId = srcMessage.channelId.value.toLong()
                this.messageId = srcMessage.id.value.toLong()
                this.roleId = role.id.value.toLong()
                this.emojiId = reactionEmoji.id.value.toLong()
                this.enabled = true
            }
        }

        askingUserForReactionInteraction.edit {
            content = "<:verified:1061586076013170719> Done!"
        }
    }
}
