package dev.slne.surf.api.core.actionbar

import dev.slne.surf.api.core.actionbar.ActionbarService.sendActionbar
import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.audience.Audience
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sends an actionbar to the audience for a specified duration.
 *
 * The audience must have an accessable UUID (see [uuidOrNull]). If the audience does not have a UUID, an error will be thrown.
 *
 * @param scope The CoroutineScope in which the actionbar will be sent.
 * @param duration The duration for which the actionbar will be displayed.
 * @param interval The interval at which the actionbar will be updated. Defaults to 1 second.
 * @param text A lambda that builds the text component to be displayed in the actionbar.
 */
fun Audience.sendActionbar(
    scope: CoroutineScope,
    duration: Duration,
    interval: Duration = 1.seconds,
    fadeOut: Boolean = false,
    text: SurfComponentBuilder.() -> Unit,
) = sendActionbar(scope, this, duration, interval, fadeOut) { SurfComponentBuilder(text) }