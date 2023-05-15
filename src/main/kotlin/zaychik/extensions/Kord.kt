package zaychik.extensions

import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder


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
