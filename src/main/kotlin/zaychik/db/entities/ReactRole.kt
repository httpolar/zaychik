package zaychik.db.entities

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import zaychik.db.tables.ReactRolesTable
import java.util.UUID

class ReactRole(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ReactRole>(ReactRolesTable)

    var guildId by ReactRolesTable.guildId
    var channelId by ReactRolesTable.channelId
    var messageId by ReactRolesTable.messageId
    var roleId by ReactRolesTable.roleId
    var emojiId by ReactRolesTable.emojiId
    var enabled by ReactRolesTable.enabled
}