package zaychik.db.entities

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import zaychik.db.tables.ButtonRoles
import java.util.*

class ButtonRole(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ButtonRole>(ButtonRoles)

    var guildId by ButtonRoles.guildId
    var channelId by ButtonRoles.channelId
    var messageId by ButtonRoles.messageId
    var roleId by ButtonRoles.roleId
}