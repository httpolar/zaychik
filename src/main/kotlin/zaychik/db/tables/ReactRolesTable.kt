package zaychik.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object ReactRolesTable : UUIDTable() {
    override val tableName: String = "react_roles"

    val guildId = long("guild_id").index()
    val channelId = long("channel_id")
    val messageId = long("message_id").index()
    val roleId = long("role_id").nullable().index()
    val emojiId = long("emoji_id")
    val enabled = bool("enabled").default(true)

    init {
        uniqueIndex(messageId, emojiId)
    }
}