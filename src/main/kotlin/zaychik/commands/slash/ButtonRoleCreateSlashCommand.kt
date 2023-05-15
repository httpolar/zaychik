package zaychik.commands.slash

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.allowedMentions
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.commands.abstracts.SlashCommand
import zaychik.db.entities.ButtonRole

class ButtonRoleCreateSlashCommand : SlashCommand() {
    override val rootName = "button-role"
    override val name = "create"
    override val description = "Creates a message with button that manages users' roles"

    companion object {
        private const val CHANNEL_OPTION = "channel"
        private const val ROLE_OPTION = "role"
        private const val MESSAGE_CONTENT_OPTION = "message"
        private const val LABEL_OPTION = "label"
    }

    override val options = mutableListOf(
        RoleBuilder(ROLE_OPTION, "A role that will be given or taken away upon button click").apply {
            required = true
        },
        ChannelBuilder(CHANNEL_OPTION, "A channel where the button role will be sent"),
        StringChoiceBuilder(LABEL_OPTION, "The button's label"),
        StringChoiceBuilder(MESSAGE_CONTENT_OPTION, "Contents of the message with the button")
    )

    override suspend fun check(event: GuildChatInputCommandInteractionCreateEvent): Boolean {
        return event.interaction.user.asMember().getPermissions().values.contains(Permission.ManageRoles)
    }

    override suspend fun action(event: GuildChatInputCommandInteractionCreateEvent) {
        val guild = event.interaction.guild

        val deferredResponse = event.interaction.deferEphemeralResponse()

        val role = event.interaction.command.roles[ROLE_OPTION]!!

        val channel = event.interaction.command.channels[CHANNEL_OPTION] ?: event.interaction.channel
        val msgContent = event.interaction.command.strings[MESSAGE_CONTENT_OPTION] ?: role.mention
        val btnLabel = event.interaction.command.strings[LABEL_OPTION] ?: "Get / Remove"

        newSuspendedTransaction(Dispatchers.IO) {
            val nextButtonRole = ButtonRole.new {
                guildId = guild.id.value
                channelId = channel.id.value
                roleId = role.id.value
            }

            val msg = event.kord.rest.channel.createMessage(channel.id) {
                content = msgContent

                actionRow {
                    interactionButton(ButtonStyle.Primary, nextButtonRole.id.toString()) {
                        label = btnLabel
                    }
                }

                allowedMentions { }
            }

            nextButtonRole.messageId = msg.id.value
            nextButtonRole
        }

        deferredResponse.respond {
            content = "Done! Button role was created in ${channel.mention}"
        }
    }
}