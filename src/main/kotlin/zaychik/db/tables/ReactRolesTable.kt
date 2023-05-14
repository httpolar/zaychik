package zaychik.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable

@OptIn(ExperimentalUnsignedTypes::class)
object ReactRolesTable : UUIDTable() {
    override val tableName: String = "react_roles"

    val guildId = ulong("guild_id").index()
    val channelId = ulong("channel_id")
    val messageId = ulong("message_id").index()
    val roleId = ulong("role_id").index()
    val emojiId = ulong("emoji_id")
    val isEmojiAnimated = bool("is_emoji_animated").default(false)
    val enabled = bool("enabled").default(true)

    init {
        uniqueIndex(messageId, emojiId)
    }
}