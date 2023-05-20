package zaychik.utils

import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.optional.value
import kotlin.reflect.KProperty

object ZaychikEmoji {
    private val emojis = listOf(
        partialEmoji("<:stopWatch:742284257958821909>"),
        partialEmoji("<:textChannel:741562093563543622>"),
        partialEmoji("<:voiceChannel:741562093546766476>"),
        partialEmoji("<:online:741562093307691051>"),
        partialEmoji("<:dnd:741562093790298153>"),
        partialEmoji("<:idle:741562093400227891>"),
        partialEmoji("<:offline:741562093647691826>"),
        partialEmoji("<:spotify:811177806293762069>"),
        partialEmoji("<:owner:741592025060605973>"),
        partialEmoji("<:members:741604275481870376>"),
    ).associateBy { it.name?.lowercase() }

    operator fun get(key: String): DiscordPartialEmoji? = emojis[key.lowercase()]

    operator fun getValue(zaychikEmoji: ZaychikEmoji, property: KProperty<*>): DiscordPartialEmoji? {
        return get(property.name.replace("_", ""))
    }

    val STOPWATCH by this
    val TEXT_CHANNEL by this
    val VOICE_CHANNEL by this
    val ONLINE by this
    val DND by this
    val IDLE by this
    val OFFLINE by this
    val SPOTIFY by this
    val OWNER by this
    val MEMBERS by this
}

fun DiscordPartialEmoji.mention(): String {
    if (this.animated.value == true) {
        return "<a:" + this.name + ":" + this.id?.value + ">"
    }
    return "<:" + this.name + ":" + this.id?.value + ">"
}