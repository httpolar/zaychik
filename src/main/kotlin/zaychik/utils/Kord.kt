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
