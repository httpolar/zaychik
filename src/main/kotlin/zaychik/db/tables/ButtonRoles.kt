package zaychik.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object ButtonRoles : UUIDTable() {
    override val tableName = "button_roles"

    val guildId = ulong("guild_id")
    val channelId = ulong("channel_id").index()
    val messageId = ulong("message_id").index()
    val roleId = ulong("role_id")
}