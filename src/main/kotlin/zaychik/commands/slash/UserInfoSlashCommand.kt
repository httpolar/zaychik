package zaychik.commands.slash

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.BooleanBuilder
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.UserBuilder
import io.github.oshai.KotlinLogging
import zaychik.commands.abstracts.SlashCommand
import zaychik.extensions.deferResponse
import zaychik.extensions.displayAvatar
import zaychik.extensions.embedBuilder
import zaychik.utils.ZaychikEmoji
import zaychik.utils.mention


class UserInfoSlashCommand : SlashCommand() {
    private val logger = KotlinLogging.logger { }

    override val name = "userinfo"
    override val description = "Shows basic information about Discord users"

    private companion object {
        const val USER_OPT = "user"
        const val IS_PRIVATE_OPT = "is_private"
    }

    override val options: MutableList<OptionsBuilder> = mutableListOf(
        UserBuilder(USER_OPT, "Discord user whose account information will be printed").apply {
            required = true
        },
        BooleanBuilder(IS_PRIVATE_OPT, "Makes Zaychik's response invisible to other users")
    )

    override suspend fun check(event: GuildChatInputCommandInteractionCreateEvent): Boolean {
        return true
    }

    override suspend fun action(event: GuildChatInputCommandInteractionCreateEvent) {
        val isPrivate = event.interaction.command.booleans[IS_PRIVATE_OPT] ?: false
        val deferredResponse = event.interaction.deferResponse(isPrivate)

        val user = event.kord.getUser(event.interaction.command.mentionables[USER_OPT]!!.id)
        if (user == null) {
            deferredResponse.respond {
                content = "User not found!"
            }
            return
        }

        val publicFlags = user.publicFlags

        val member = user.asMemberOrNull(event.interaction.guildId)
        val activities = member?.getPresenceOrNull()?.activities

        val embed = embedBuilder {
            thumbnail {
                url = user.displayAvatar().cdnUrl.toUrl()
            }

            field {
                inline = true
                name = "User tag"
                value = user.tag
            }

            field {
                inline = true
                name = "ID"
                value = "${user.id.value}"
            }

            field {
                inline = true
                name = "Avatar"
                value = "[Open URL](" + user.displayAvatar().cdnUrl.toUrl() + ")"
            }

            if (user.banner != null) {
                field {
                    inline = true
                    name = "Banner"
                    value = "[Open URL](" + user.banner?.cdnUrl?.toUrl() + ")"
                }
            }

            val accentColor = user.accentColor
            if (accentColor != null) {
                field {
                    inline = true
                    name = "Accent Colour"
                    value = String.format(
                        "#%02x%02x%02x",
                        accentColor.red,
                        accentColor.green,
                        accentColor.blue
                    )
                }
            }

            if (publicFlags != null && publicFlags.flags.isNotEmpty()) {
                field {
                    inline = false
                    name = "User Flags"
                    value = publicFlags.flags.joinToString(separator = "\n") { it.name }
                }
            }


            if (!activities.isNullOrEmpty()) {
                activities.forEach {
                    field {
                        inline = false
                        name = "${it.name} ${it.type}"
                        value = "${it.details}"
                    }
                }
            }

            if (member != null && member.roleIds.isNotEmpty()) {
                field {
                    inline = false
                    name = "Roles"
                    value = member.roleBehaviors.joinToString(" ") { "<@&${it.id.value}>" }
                }
            }

            if (member != null) {
                field {
                    inline = false
                    name = "Server join date"
                    value = "${ZaychikEmoji.STOPWATCH?.mention()} <t:" + member.joinedAt.epochSeconds + ":F>"
                }
            }

            field {
                name = "Account creation date"
                value = "${ZaychikEmoji.STOPWATCH?.mention()} <t:" + user.id.timestamp.epochSeconds + ":F>"
            }
        }

        deferredResponse.respond {
            embeds = mutableListOf(embed)
        }
    }
}