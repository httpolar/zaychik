package zaychik.commands.slash

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.BooleanBuilder
import dev.kord.rest.builder.interaction.MentionableBuilder
import dev.kord.rest.builder.interaction.OptionsBuilder
import zaychik.commands.abstracts.SlashCommand
import zaychik.extensions.deferResponse
import zaychik.extensions.displayAvatar
import zaychik.extensions.embedBuilder


class UserInfoSlashCommand : SlashCommand() {
    override val name = "userinfo"
    override val description = "Shows basic information about Discord users"

    private companion object {
        const val USER_OPT = "user"
        const val IS_PRIVATE_OPT = "is_private"
    }

    override val options: MutableList<OptionsBuilder> = mutableListOf(
        MentionableBuilder(USER_OPT, "Discord user whose account information will be printed").apply {
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

        val embed = embedBuilder {
            title = "${user.username}#${user.discriminator}"

            thumbnail {
                url = user.displayAvatar().cdnUrl.toUrl()
            }

            field {
                inline = true
                name = "Name"
                value = user.username
            }

            field {
                inline = true
                name = "Discrim"
                value = user.discriminator
            }

            field {
                inline = true
                name = "Created on"
                value = "<t:" + user.id.timestamp.epochSeconds + ":D>"
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

            if (user.publicFlags != null) {
                field {
                    inline = true
                    name = "Public Flags"
                    value = user.publicFlags?.flags?.joinToString(separator = ", ") ?: "no flags :("
                }
            }
        }

        deferredResponse.respond {
            embeds = mutableListOf(embed)
        }
    }
}