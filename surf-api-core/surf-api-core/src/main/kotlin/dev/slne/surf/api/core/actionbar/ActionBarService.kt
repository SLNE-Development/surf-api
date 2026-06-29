package dev.slne.surf.api.core.actionbar

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.requiredService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface ActionBarService {

    /**
     * Sends an action bar to the given [audience] repeatedly.
     *
     * The [computation] callback is invoked once per tick to build the component that should be
     * sent for the current update. If [computation] throws, the exception is logged and the current
     * tick is skipped.
     *
     * The returned [Job] can be cancelled to stop the action bar early. When the task finishes,
     * [onFinish] is invoked with an [ActionBarFinishReason] describing why it ended.
     *
     * If [fadeOut] is `false`, an empty action bar is sent after the task completes normally or is
     * auto-cancelled. If [fadeOut] is `true`, the last sent action bar is allowed to disappear
     * naturally on the client.
     *
     * If [autoCancelPlayer] is `true` and the [audience] is a player, the task automatically stops
     * once that player is no longer online.
     *
     * @param scope The coroutine scope used to launch the repeating action bar task.
     * @param audience The audience that should receive the action bar.
     * @param duration The total duration for which the action bar should be updated.
     * @param interval The delay between two action bar updates.
     * @param fadeOut Whether the last action bar should fade out naturally instead of being cleared.
     * @param autoCancelPlayer Whether to stop automatically when a player audience goes offline.
     * @param computation Computes the component to send for the current tick.
     * @param afterTick Optional callback invoked after each tick, regardless of whether a component was sent.
     * @param onFinish Optional callback invoked when the action bar task finishes.
     *
     * @return The launched action bar job.
     *
     * @throws IllegalArgumentException If [duration] or [interval] is not positive.
     */
    fun sendActionBar(
        scope: CoroutineScope,
        audience: Audience,
        duration: Duration,
        interval: Duration = 1.seconds,
        fadeOut: Boolean = false,
        autoCancelPlayer: Boolean = true,
        computation: () -> Component,
        afterTick: (() -> Unit)? = null,
        onFinish: ((ActionBarFinishReason) -> Unit)? = null
    ): Job

    companion object : ActionBarService by INSTANCE {
        val instance get() = INSTANCE
    }
}

private val INSTANCE = requiredService<ActionBarService>()

/**
 * Sends a repeatedly updated action bar to this [Audience].
 *
 * This overload supports [afterTick] and [onFinish] callbacks. Because [textComputation] is not
 * the last parameter, callers usually need to pass it as a named argument when using callbacks.
 *
 * Example:
 * ```kotlin
 * player.sendActionBar(
 *     scope = scope,
 *     duration = 5.seconds,
 *     textComputation = {
 *         info("Loading...")
 *     },
 *     onFinish = { reason ->
 *         // handle finish reason
 *     }
 * )
 * ```
 *
 * @param scope The coroutine scope used to launch the repeating action bar task.
 * @param duration The total duration for which the action bar should be updated.
 * @param interval The delay between two action bar updates.
 * @param fadeOut Whether the last action bar should fade out naturally instead of being cleared.
 * @param autoCancelPlayer Whether to stop automatically when this audience is an offline player.
 * @param textComputation Builds the action bar component for the current tick.
 * @param afterTick Optional callback invoked after each tick.
 * @param onFinish Optional callback invoked when the action bar task finishes.
 *
 * @return The launched action bar job.
 */
fun Audience.sendActionBar(
    scope: CoroutineScope,
    duration: Duration,
    interval: Duration = 1.seconds,
    fadeOut: Boolean = false,
    autoCancelPlayer: Boolean = true,
    textComputation: SurfComponentBuilder.() -> Unit,
    afterTick: (() -> Unit)? = null,
    onFinish: ((ActionBarFinishReason) -> Unit)? = null
) = ActionBarService.instance.sendActionBar(
    scope,
    this,
    duration,
    interval,
    fadeOut,
    autoCancelPlayer,
    { SurfComponentBuilder(textComputation) },
    afterTick,
    onFinish
)

/**
 * Sends a repeatedly updated action bar to this [Audience].
 *
 * This overload keeps [textComputation] as the trailing lambda, making simple action bars concise:
 *
 * ```kotlin
 * player.sendActionBar(scope, 5.seconds) {
 *     info("Loading...")
 * }
 * ```
 *
 * Use the overload with [afterTick] and [onFinish] parameters when callbacks are required.
 *
 * @param scope The coroutine scope used to launch the repeating action bar task.
 * @param duration The total duration for which the action bar should be updated.
 * @param interval The delay between two action bar updates.
 * @param fadeOut Whether the last action bar should fade out naturally instead of being cleared.
 * @param autoCancelPlayer Whether to stop automatically when this audience is an offline player.
 * @param textComputation Builds the action bar component for the current tick.
 *
 * @return The launched action bar job.
 */
fun Audience.sendActionBar(
    scope: CoroutineScope,
    duration: Duration,
    interval: Duration = 1.seconds,
    fadeOut: Boolean = false,
    autoCancelPlayer: Boolean = true,
    textComputation: SurfComponentBuilder.() -> Unit
) = ActionBarService.instance.sendActionBar(
    scope,
    this,
    duration,
    interval,
    fadeOut,
    autoCancelPlayer,
    { SurfComponentBuilder(textComputation) },
    null,
    null
)