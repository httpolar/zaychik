package zaychik.db.entities

import dev.kord.core.entity.ReactionEmoji
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import zaychik.db.tables.ReactRolesTable
import java.util.UUID

class ReactRole(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ReactRole>(ReactRolesTable) {
        @JvmStatic
        suspend fun fromReactionEmoji(reaction: ReactionEmoji?, messageId: ULong): ReactRole? {
            val emoji = (reaction as? ReactionEmoji.Custom) ?: return null
            val emojiId = emoji.id.value

            val reactRole = newSuspendedTransaction(Dispatchers.IO) {
                ReactRole.find { (ReactRolesTable.messageId eq messageId) and (ReactRolesTable.emojiId eq emojiId) }
                    .limit(1)
                    .firstOrNull()
            } ?: return null

            return reactRole
        }
    }

    var guildId by ReactRolesTable.guildId
    var channelId by ReactRolesTable.channelId
    var messageId by ReactRolesTable.messageId
    var roleId by ReactRolesTable.roleId
    var emojiId by ReactRolesTable.emojiId
    var enabled by ReactRolesTable.enabled
}

