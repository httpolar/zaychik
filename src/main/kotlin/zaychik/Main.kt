package zaychik

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.channel.ChannelDeleteEvent
import dev.kord.core.event.interaction.*
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.rest.builder.interaction.role
import dev.kord.rest.builder.interaction.subCommand
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import zaychik.commands.app.CreateReactRoleAppCommand
import zaychik.commands.app.ViewReactRolesAppCommand
import zaychik.db.ZaychikDatabase
import zaychik.db.entities.ReactRole
import zaychik.db.entities.fromReactionEmoji
import zaychik.db.tables.ReactRolesTable

private val logger = KotlinLogging.logger {}


fun zaychikModule() = module {
    singleOf(::kordFactory)
    singleOf(::Zaychik)
}

fun kordFactory() = runBlocking {
    logger.info("Created an instance of Kord")
    return@runBlocking Kord(Config.Bot.token)
}

class Zaychik(private val kord: Kord) {
    private val appCommands = mapOf(
        CreateReactRoleAppCommand.name to CreateReactRoleAppCommand(),
        ViewReactRolesAppCommand.name to ViewReactRolesAppCommand(),
    )

    private suspend fun createAppCommands() {
        kord.createGlobalApplicationCommands {
            appCommands.keys.forEach {
                message(name = it)
            }
        }
    }

    private suspend fun createSlashCommands() {
        kord.createGlobalChatInputCommand(name = "react-role", description = "Reaction roles") {
            subCommand(name = "create", description = "Creates a reaction role") {
                role(name = "role", description = "Role that will be given to the user who clicks the reaction")
            }
            subCommand(name = "delete", description = "Deletes a reaction role")
            subCommand(name = "find", description = "Searches for a reaction role or a message with it")
        }
    }

    private suspend fun extractRoleFromReaction(
        guild: GuildBehavior?,
        message: MessageBehavior,
        emoji: ReactionEmoji,
        block: suspend (RoleBehavior) -> Unit
    ) {
        if (guild == null) return
        val messageId = message.id.value

        val reactRoleId = ReactRole
            .fromReactionEmoji(emoji, messageId)
            ?.roleId
            ?.let(::Snowflake)
            ?: return

        val role = guild.roles.firstOrNull { r -> r.id == reactRoleId } ?: return

        block(role)
    }

    suspend fun start() {
        logger.info("Zaychik is starting!")

        ZaychikDatabase.connect()
        newSuspendedTransaction(Dispatchers.IO) {
            if (!ReactRolesTable.exists()) {
                SchemaUtils.createMissingTablesAndColumns(ReactRolesTable)
            }
        }

        createAppCommands()

        kord.on<GuildMessageCommandInteractionCreateEvent> {
            val cmd = appCommands.getOrDefault(interaction.invokedCommandName, null)
                ?: return@on

            cmd.checkAndRun(this) {
                interaction.respondEphemeral {
                    content = ":x: Missing permissions! You are not allowed to run this command."
                }
            }
        }

        kord.on<ReactionAddEvent> {
            extractRoleFromReaction(guild, message, emoji) {
                userAsMember?.addRole(it.id, "(+) Reaction Role Added")
            }
        }

        kord.on<ReactionRemoveEvent> {
            extractRoleFromReaction(guild, message, emoji) {
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

suspend fun main() {
    Config.load("config.properties")  // This will use config.properties of the working directory

    val koin = startKoin {
        modules(zaychikModule())
    }.koin

    koin.get<Zaychik>().start()
}