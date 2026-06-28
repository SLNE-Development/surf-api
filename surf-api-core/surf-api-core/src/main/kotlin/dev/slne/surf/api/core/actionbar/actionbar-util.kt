package dev.slne.surf.api.core.actionbar

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
 * @param onCount An optional callback that is invoked each time the actionbar is sent.
 * @param onFinish An optional callback that is invoked when the actionbar display is finished.
 */
fun Audience.sendActionbar(
    scope: CoroutineScope,
    duration: Duration,
    interval: Duration = 1.seconds,
    fadeOut: Boolean = false,
    text: SurfComponentBuilder.() -> Unit,
    onCount: (() -> Unit)? = null,
    onFinish: (() -> Unit)? = null
) = ActionbarService.sendActionbar(
    scope,
    this,
    duration,
    interval,
    fadeOut,
    { SurfComponentBuilder(text) },
    onCount,
    onFinish
)

/**
 * Sends an actionbar to the audience for a specified duration.
 *
 * The audience must have an accessible UUID (see [uuidOrNull]). If the audience does not have a UUID, an error will be thrown.
 *
 * @param scope The CoroutineScope in which the actionbar will be sent.
 * @param duration The duration for which the actionbar will be displayed.
 * @param interval The interval at which the actionbar will be updated. Defaults to 1 second.
 * @param fadeOut When true, lets the actionbar disappear naturally after the last update; when false, clears it immediately when finished.
 * @param text A lambda that builds the text component to be displayed in the actionbar.
 */
fun Audience.sendActionbar(
    scope: CoroutineScope,
    duration: Duration,
    interval: Duration = 1.seconds,
    fadeOut: Boolean = false,
    text: SurfComponentBuilder.() -> Unit
) = ActionbarService.sendActionbar(
    scope,
    this,
    duration,
    interval,
    fadeOut,
    { SurfComponentBuilder(text) },
    null,
    null
)