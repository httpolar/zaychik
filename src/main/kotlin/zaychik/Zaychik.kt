package zaychik

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.Role
import dev.kord.core.event.channel.ChannelDeleteEvent
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.commands.app.CreateReactRoleAppCommand
import zaychik.commands.app.DeleteReactRolesAppCommand
import zaychik.commands.app.ViewReactRolesAppCommand
import zaychik.commands.executableCommand
import zaychik.db.ZaychikDatabase
import zaychik.db.entities.ReactRole
import zaychik.db.tables.ReactRolesTable
import java.util.*

class Zaychik(private val kord: Kord) {
    private val logger = KotlinLogging.logger {}

    private val appCommands = mapOf(
        CreateReactRoleAppCommand.name to CreateReactRoleAppCommand(),
        ViewReactRolesAppCommand.name to ViewReactRolesAppCommand(),
        DeleteReactRolesAppCommand.name to DeleteReactRolesAppCommand(),
    )

    private suspend fun createAppCommands() {
        kord.createGlobalApplicationCommands {
            appCommands.keys.forEach {
                message(name = it)
            }
        }
    }

    private suspend fun extractRoleFromReaction(
        guild: GuildBehavior?,
        message: MessageBehavior,
        emoji: ReactionEmoji,
        block: (suspend (RoleBehavior) -> Unit)? = null
    ): Optional<Role> {
        if (guild == null) return Optional.empty()
        val messageId = message.id.value

        val reactRoleId = ReactRole
            .fromReactionEmoji(emoji, messageId)
            ?.roleId
            ?.let(::Snowflake)
            ?: return Optional.empty()

        val role = guild.roles.firstOrNull { r -> r.id == reactRoleId } ?: return Optional.empty()

        if (block != null) {
            block(role)
        }

        return Optional.of(role)
    }

    suspend fun start() {
        logger.info("Zaychik is starting!")

        ZaychikDatabase.connect()
        newSuspendedTransaction(Dispatchers.IO) {
            SchemaUtils.createMissingTablesAndColumns(ReactRolesTable)
        }

        createAppCommands()

        kord.on<GuildMessageCommandInteractionCreateEvent> {
            val cmd = appCommands.getOrDefault(interaction.invokedCommandName, null)
                ?: return@on

            executableCommand(this)
                .command(cmd)
                .onCheckFailure {
                    interaction.respondEphemeral {
                        content = ":x: Missing permissions! You are not allowed to run this command."

                    }
                }
                .execute()
        }

        kord.on<ReactionAddEvent> {
            extractRoleFromReaction(guild, message, emoji) {
                userAsMember?.addRole(it.id, "(+) Reaction Role Added")
            }
        }

        kord.on<ReactionRemoveEvent> {
            extractRoleFromReaction(guild, message, emoji) {
                if (userAsMember?.asMember()?.roleIds?.contains(it.id) != true) {
                    return@extractRoleFromReaction
                }

                userAsMember?.removeRole(it.id, "(-) Reaction Role Removed")
            }
        }

        kord.on<MessageDeleteEvent> {
            val eventMessageId = messageId.value
            newSuspendedTransaction(Dispatchers.IO) {
                ReactRolesTable.deleteWhere { messageId eq eventMessageId }
            }
        }

        kord.on<ChannelDeleteEvent> {
            val eventChannelId = channel.id.value
            newSuspendedTransaction(Dispatchers.IO) {
                ReactRolesTable.deleteWhere { channelId eq eventChannelId }
            }
        }

        kord.login {
            intents = Intents.nonPrivileged
        }
    }
}
