package zaychik.utils

import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.OptionalBoolean

fun partialEmoji(id: Snowflake, name: String = "x", isAnimated: Boolean = false): DiscordPartialEmoji {
    return DiscordPartialEmoji(id, name, OptionalBoolean.Value(isAnimated))
}

fun partialEmoji(id: ULong, name: String = "x", isAnimated: Boolean = false): DiscordPartialEmoji {
    return partialEmoji(Snowflake(id), name, isAnimated)
}

fun partialEmoji(emoji: String): DiscordPartialEmoji {
    val match = """<(?<isAnimated>a?):(?<name>[a-zA-Z_\-\d]+):(?<id>\d+)>""".toRegex().matchEntire(emoji)
        ?: error("invalid custom emoji string")

    val id = match.groups["id"]?.value?.toULongOrNull()
        ?: error("couldn't parse emoji id which is required to build a partial emoji")

    val name = match.groups["name"]?.value ?: "emoji"
    val isAnimated = match.groups["isAnimated"]?.value == "a"

    return partialEmoji(id, name, isAnimated)
}