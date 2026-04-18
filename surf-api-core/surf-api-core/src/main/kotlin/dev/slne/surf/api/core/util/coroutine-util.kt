package dev.slne.surf.api.core.util

import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Starts a coroutine that executes [block] at a fixed rate.
 *
 * The next execution time is calculated based on a fixed schedule, independent of how long
 * [block] takes to execute. If [block] runs longer than [period], subsequent executions
 * will attempt to "catch up" without additional delay.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution (if > 0).
 * - Executes [block] immediately after the initial delay.
 * - Subsequent executions are aligned to a fixed interval defined by [period].
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 *
 * Error handling:
 * - If [block] throws an exception, the coroutine is cancelled and no further executions occur.
 *
 * @param period the interval between scheduled executions; must be > 0
 * @param initialDelay the delay before the first execution; must be >= 0
 * @param block the suspending block to execute repeatedly
 *
 * @return the [Job] representing the running coroutine
 *
 * @throws IllegalArgumentException if [period] is not positive or [initialDelay] is negative
 */
fun CoroutineScope.runAtFixedRate(
    period: Duration,
    initialDelay: Duration = Duration.ZERO,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    require(period > Duration.ZERO) { "period must be positive" }
    require(initialDelay >= Duration.ZERO) { "initialDelay must not be negative" }

    if (initialDelay.isPositive()) {
        delay(initialDelay)
        ensureActive()
    }

    var nextRun = System.nanoTime()
    while (isActive) {
        nextRun += period.inWholeNanoseconds

        block()
        ensureActive()

        val waitNanos = nextRun - System.nanoTime()
        if (waitNanos > 0) {
            delay(waitNanos.nanoseconds)
        }
    }
}

/**
 * Starts a coroutine that executes [block] with a fixed delay between executions.
 *
 * The delay is applied *after* each execution of [block]. This means that the time between
 * the start of consecutive executions depends on how long [block] takes to run.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution (if > 0).
 * - Executes [block].
 * - Waits for [delay] after each execution before starting the next one.
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 *
 * Error handling:
 * - If [block] throws an exception, the coroutine is cancelled and no further executions occur.
 *
 * @param delay the delay between executions; must be > 0
 * @param initialDelay the delay before the first execution; must be >= 0
 * @param block the suspending block to execute repeatedly
 *
 * @return the [Job] representing the running coroutine
 *
 * @throws IllegalArgumentException if [delay] is not positive or [initialDelay] is negative
 */
fun CoroutineScope.runWithFixedDelay(
    delay: Duration,
    initialDelay: Duration = Duration.ZERO,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    require(delay > Duration.ZERO) { "delay must be positive" }
    require(initialDelay >= Duration.ZERO) { "initialDelay must not be negative" }

    if (initialDelay.isPositive()) {
        delay(initialDelay)
        ensureActive()
    }

    while (isActive) {
        block()
        ensureActive()
        delay(delay)
    }
}

/**
 * Starts a coroutine that repeatedly executes [block] with a fixed [delay] between executions
 * as long as [predicate] returns `true`.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution (if > 0).
 * - Before each iteration, [predicate] is evaluated.
 * - If [predicate] returns `true`, [block] is executed.
 * - After execution, waits for [delay] before the next iteration.
 * - Stops when [predicate] returns `false` or the coroutine is cancelled.
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 *
 * Error handling:
 * - If [block] or [predicate] throws an exception, the coroutine is cancelled
 *   and no further executions occur.
 *
 * @param delay the delay between executions; must be >= 0
 * @param initialDelay the delay before the first execution; must be >= 0
 * @param predicate condition that controls whether execution should continue
 * @param block the suspending block to execute repeatedly
 *
 * @return the [Job] representing the running coroutine
 *
 * @throws IllegalArgumentException if [delay] or [initialDelay] is negative
 */
fun CoroutineScope.runUntil(
    delay: Duration,
    initialDelay: Duration = Duration.ZERO,
    predicate: suspend () -> Boolean,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    require(delay >= Duration.ZERO) { "delay must not be negative" }
    require(initialDelay >= Duration.ZERO) { "initialDelay must not be negative" }

    if (initialDelay.isPositive()) {
        delay(initialDelay)
        ensureActive()
    }

    while (isActive && predicate()) {
        block()
        ensureActive()
        delay(delay)
    }
}