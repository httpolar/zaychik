package zaychik.db.entities

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import zaychik.db.tables.ReactRolesTable
import java.util.UUID

class ReactRole(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ReactRole>(ReactRolesTable)

    val guildId by ReactRolesTable.guildId
    val channelId by ReactRolesTable.channelId
    val messageId by ReactRolesTable.messageId
    val roleId by ReactRolesTable.roleId
    val emojiId by ReactRolesTable.emojiId
    val enabled by ReactRolesTable.enabled
}