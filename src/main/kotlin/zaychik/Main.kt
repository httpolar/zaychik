package zaychik

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
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

    private suspend fun createContextualCommands() {
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
        val messageId = message.id.value.toLong()

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

        createContextualCommands()

        // TODO: Handle unknown roles (users might delete a role but keep react role)
        kord.on<ReactionAddEvent> {
            val guild = guild ?: return@on
            val messageId = message.id.value.toLong()

            val reactRoleId = ReactRole
                .fromReactionEmoji(emoji, messageId)
                ?.roleId
                ?.let(::Snowflake)
                ?: return@on

            kord.rest.guild.addRoleToGuildMember(
                guildId = guild.id,
                userId = user.id,
                roleId = reactRoleId,
                reason = "+Reaction role"
            )
        }

        // TODO: Handle unknown roles (users might delete a role but keep react role)
        kord.on<ReactionRemoveEvent> {
            val guild = guild ?: return@on
            val messageId = message.id.value.toLong()

            val reactRoleId = ReactRole
                .fromReactionEmoji(emoji, messageId)
                ?.roleId
                ?.let(::Snowflake)
                ?: return@on

            kord.rest.guild.deleteRoleFromGuildMember(
                guildId = guild.id,
                userId = user.id,
                roleId = reactRoleId,
                reason = "-Reaction role"
            )
        }

        kord.on<GuildMessageCommandInteractionCreateEvent> {
            val cmd = appCommands.getOrDefault(interaction.invokedCommandName, null)
                ?: return@on

            val canRun = cmd.check(this)
            if (!canRun) {
                interaction.respondEphemeral {
                    content = ":x: Missing permissions! You are not allowed to run this command."
                }
                return@on
            }
            cmd.action(this)
        }

        kord.on<MessageDeleteEvent> {
            val eventMessageId = messageId.value.toLong()
            newSuspendedTransaction(Dispatchers.IO) {
                ReactRolesTable.deleteWhere { messageId eq eventMessageId }
            }
        }

        kord.on<ChannelDeleteEvent> {
            val eventChannelId = channel.id.value.toLong()
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