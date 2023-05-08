package zaychik

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.*
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.rest.builder.interaction.role
import dev.kord.rest.builder.interaction.subCommand
import io.github.oshai.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import zaychik.commands.contextual.ReactRoleContextCommand
import zaychik.db.ZaychikDatabase
import zaychik.db.entities.ReactRole
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
    private val contextualCommands = mapOf(
        ReactRoleContextCommand.name to ReactRoleContextCommand(),
    )

    private suspend fun createContextualCommands() {
        kord.createGlobalApplicationCommands {
            contextualCommands.keys.forEach {
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

    suspend fun start() {
        logger.info("Zaychik is starting!")

        ZaychikDatabase.connect()
        newSuspendedTransaction(Dispatchers.IO) {
            if (!ReactRolesTable.exists()) {
                SchemaUtils.createMissingTablesAndColumns(ReactRolesTable)
            }
        }

        createContextualCommands()

        kord.on<GuildMessageCommandInteractionCreateEvent> {
            val cmd = contextualCommands.getOrDefault(interaction.invokedCommandName, null)
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

        kord.on<MessageDeleteEvent>  {
            newSuspendedTransaction(Dispatchers.IO) {
                ReactRole.find { ReactRolesTable.messageId eq messageId.value.toLong() }
                    .firstOrNull()
                    ?.delete()
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