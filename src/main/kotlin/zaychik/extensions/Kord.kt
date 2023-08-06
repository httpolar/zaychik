package zaychik.extensions

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.entity.User
import dev.kord.core.event.Event
import dev.kord.rest.builder.message.EmbedBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout


suspend fun ActionInteractionBehavior.deferResponse(isEphemeral: Boolean = false): DeferredMessageInteractionResponseBehavior {
    return when (isEphemeral) {
        true -> deferEphemeralResponse()
        false -> deferPublicResponse()
    }
}

fun embedBuilder(block: EmbedBuilder.() -> Unit = {}): EmbedBuilder {
    return EmbedBuilder().apply(block)
}

fun User.displayAvatar() = avatar ?: defaultAvatar

@OptIn(FlowPreview::class)
suspend inline fun <reified T : Event> Kord.waitFor(
    timeout: Duration = 90.seconds,
    capacity: Int = Channel.UNLIMITED,
    crossinline predicate: suspend (T) -> Boolean
): Result<T> {
    return runCatching {
        events.buffer(capacity)
            .filterIsInstance<T>()
            .filter(predicate)
            .timeout(timeout)
            .first()
    }
}