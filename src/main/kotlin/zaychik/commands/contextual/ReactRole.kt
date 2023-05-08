package zaychik.commands.contextual

import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildModalSubmitInteractionCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.db.entities.ReactRole
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
suspend fun handleReactRoleCreateFromRightClick(event: GuildMessageCommandInteractionCreateEvent) {
    val kord = event.kord
    val actor = event.interaction.user
    val guild = event.interaction.guild

    val originalMessage = event.interaction.messages.values.firstOrNull() ?:
    // tell user things may fuck up sometimes
    return

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

    val modalCustomId = UUID.randomUUID().toString()
    val roleIdFieldId = "$modalCustomId:role"
    val emojiFieldId = "$modalCustomId:emoji"

    event.interaction.modal("Reaction Role Creator", modalCustomId) {
        actionRow {
            textInput(TextInputStyle.Short, roleIdFieldId, "Role ID") {
                placeholder = "1061431192802570380"
            }
        }
        actionRow {
            textInput(TextInputStyle.Short, emojiFieldId, "Emoji in format <name:id>") {
                placeholder = "<:SeeleBlush:1104700277141024768>"
            }
        }
    }


    val submission = kord.events.buffer(Channel.UNLIMITED)
        .filterIsInstance<GuildModalSubmitInteractionCreateEvent?>()
        .filter { e -> e?.interaction?.user?.id == actor.id }
        .take(1)
        .timeout(90.seconds)
        .catch { emit(null) }
        .firstOrNull()
        ?: // tell user something fucked up
        return

    val providedRoleId = submission.interaction.textInputs.getOrDefault(roleIdFieldId, null)?.value?.trim()
    val providedEmoji = submission.interaction.textInputs.getOrDefault(emojiFieldId, null)?.value?.trim()

    if (providedRoleId == null || providedEmoji == null) {
        submission.interaction.respondEphemeral {
            content = ":x: You must provide role id and the correct emoji representation!"
        }
        return
    }

    val roleId = providedRoleId.toLongOrNull()
    if (roleId == null) {
        submission.interaction.respondEphemeral {
            content = ":x: Provided role id is incorrect number representation!"
        }
        return
    }

    val role = try {
        guild.getRole(Snowflake(roleId)).fetchRole()
    } catch (e: EntityNotFoundException) {
        null
    }

    if (role == null) {
        submission.interaction.respondEphemeral {
            content = ":x: You have provided a non-existent role!"
        }
        return
    }

    val emojiMatch = """<(?<animated>a)?:(?<name>[a-zA-Z\d_]+):(?<id>\d+)>""".toRegex().matchEntire(providedEmoji)

    val emojiName = emojiMatch?.groups?.get("name")?.value
    val emojiId = emojiMatch?.groups?.get("id")?.value?.toLongOrNull()?.let { Snowflake(it) }

    if (emojiName == null || emojiId == null) {
        submission.interaction.respondEphemeral {
            content = ":x: You have provided an incorrect emoji! Make sure it follows the formatting."
        }
        return
    }

    val emoji = guild.emojis.buffer(Channel.UNLIMITED)
        .filter { e -> e.name == emojiName && e.id == emojiId }
        .take(1)
        .firstOrNull()

    if (emoji == null) {
        submission.interaction.respondEphemeral {
            content = ":x: You have provided a non-existent emoji!"
        }
        return
    }

    originalMessage.addReaction(emoji)

    newSuspendedTransaction(Dispatchers.IO) {
        ReactRole.new {
            this.guildId = guild.id.value.toLong()
            this.channelId = originalMessage.channelId.value.toLong()
            this.messageId = originalMessage.id.value.toLong()
            this.roleId = role.id.value.toLong()
            this.emojiId = emoji.id.value.toLong()
            this.enabled = true

        }
    }

    submission.interaction.respondEphemeral {
        content = "<:verified:1061586076013170719> Done!"
    }
}
