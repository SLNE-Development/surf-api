@file:OptIn(ExperimentalVersionOverloading::class)

package dev.slne.surf.api.core.util

import kotlinx.coroutines.*
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

private data class TaskLogging(
    val name: String,
    val logger: ComponentLogger,
)

private fun createTaskLogging(
    defaultName: String,
    taskName: String?,
): TaskLogging {
    val normalizedTaskName = taskName?.takeIf { it.isNotBlank() }
    val resolvedName = normalizedTaskName ?: defaultName
    val logger = getCallerClass(1)?.let(ComponentLogger::logger) ?: ComponentLogger.logger(defaultName)

    return TaskLogging(
        name = resolvedName,
        logger = logger,
    )
}

private fun CoroutineScope.handleTaskException(
    throwable: Throwable,
    logging: TaskLogging,
    catchExceptions: Boolean,
) {
    if (throwable is CancellationException) {
        ensureActive()
        logging.logger.warn("A Task tried to cancel itself while the job was still active. Ignoring.")
        return
    }

    if (catchExceptions) {
        logging.logger.error("Exception in ${logging.name}", throwable)
    } else {
        throw throwable
    }
}

/**
 * Starts a coroutine that executes [block] at a fixed rate.
 *
 * The next execution time is calculated based on a fixed schedule, independent of how long
 * [block] takes to execute. If [block] runs longer than [period], subsequent executions
 * will attempt to "catch up" without additional delay.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution if it is greater than zero.
 * - Executes [block] immediately after the initial delay.
 * - Subsequent executions are aligned to a fixed interval defined by [period].
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 * - If [block] throws a [CancellationException] while the job is still active, the exception
 *   is treated as a self-cancellation attempt, logged as a warning, and ignored.
 *
 * Error handling:
 * - If [catchExceptions] is `true`, non-cancellation exceptions thrown by [block] are logged
 *   and the next execution is still attempted.
 * - If [catchExceptions] is `false`, non-cancellation exceptions thrown by [block] are rethrown
 *   and the coroutine fails.
 *
 * Logging:
 * - The logger is resolved before the coroutine is launched.
 * - The caller class is used as logger when available.
 * - If the caller class cannot be resolved, `runAtFixedRate` is used as fallback logger name.
 *
 * @param period the interval between scheduled executions; must be greater than zero
 * @param initialDelay the delay before the first execution; must not be negative
 * @param catchExceptions whether non-cancellation exceptions should be logged and swallowed
 * @param taskName optional task name used for logging
 * @param block the suspending block to execute repeatedly
 *
 * @return the [Job] representing the running coroutine
 *
 * @throws IllegalArgumentException if [period] is not positive or [initialDelay] is negative
 */
fun CoroutineScope.runAtFixedRate(
    period: Duration,
    initialDelay: Duration = Duration.ZERO,
    @IntroducedAt("3.29.0") catchExceptions: Boolean = true,
    @IntroducedAt("3.29.0") taskName: String? = null,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    require(period > Duration.ZERO) { "period must be positive" }
    require(initialDelay >= Duration.ZERO) { "initialDelay must not be negative" }

    val logging = createTaskLogging(
        defaultName = "runAtFixedRate",
        taskName = taskName,
    )

    return launch {
        if (initialDelay.isPositive()) {
            delay(initialDelay)
            ensureActive()
        }

        var nextRun = System.nanoTime()
        while (isActive) {
            nextRun += period.inWholeNanoseconds

            try {
                block()
            } catch (throwable: Throwable) {
                handleTaskException(
                    throwable = throwable,
                    logging = logging,
                    catchExceptions = catchExceptions,
                )
            }

            ensureActive()

            val waitNanos = nextRun - System.nanoTime()
            if (waitNanos > 0) {
                delay(waitNanos.nanoseconds)
            }
        }
    }
}

/**
 * Starts a coroutine that executes [block] with a fixed delay between executions.
 *
 * The delay is applied after each execution of [block]. This means that the time between
 * the start of consecutive executions depends on how long [block] takes to run.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution if it is greater than zero.
 * - Executes [block].
 * - Waits for [delay] after each execution before starting the next one.
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 * - If [block] throws a [CancellationException] while the job is still active, the exception
 *   is treated as a self-cancellation attempt, logged as a warning, and ignored.
 *
 * Error handling:
 * - If [catchExceptions] is `true`, non-cancellation exceptions thrown by [block] are logged
 *   and the next execution is still attempted.
 * - If [catchExceptions] is `false`, non-cancellation exceptions thrown by [block] are rethrown
 *   and the coroutine fails.
 *
 * Logging:
 * - The logger is resolved before the coroutine is launched.
 * - The caller class is used as logger when available.
 * - If the caller class cannot be resolved, `runWithFixedDelay` is used as fallback logger name.
 *
 * @param delay the delay between executions; must be greater than zero
 * @param initialDelay the delay before the first execution; must not be negative
 * @param catchExceptions whether non-cancellation exceptions should be logged and swallowed
 * @param taskName optional task name used for logging
 * @param block the suspending block to execute repeatedly
 *
 * @return the [Job] representing the running coroutine
 *
 * @throws IllegalArgumentException if [delay] is not positive or [initialDelay] is negative
 */
fun CoroutineScope.runWithFixedDelay(
    delay: Duration,
    initialDelay: Duration = Duration.ZERO,
    @IntroducedAt("3.29.0") catchExceptions: Boolean = true,
    @IntroducedAt("3.29.0") taskName: String? = null,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    require(delay > Duration.ZERO) { "delay must be positive" }
    require(initialDelay >= Duration.ZERO) { "initialDelay must not be negative" }

    val logging = createTaskLogging(
        defaultName = "runWithFixedDelay",
        taskName = taskName,
    )

    return launch {
        if (initialDelay.isPositive()) {
            delay(initialDelay)
            ensureActive()
        }

        while (isActive) {
            try {
                block()
            } catch (throwable: Throwable) {
                handleTaskException(
                    throwable = throwable,
                    logging = logging,
                    catchExceptions = catchExceptions,
                )
            }

            ensureActive()
            delay(delay)
        }
    }
}

/**
 * Starts a coroutine that repeatedly executes [block] with a fixed [delay] between executions
 * as long as [predicate] returns `true`.
 *
 * This overload keeps the original call shape for usages with two lambdas and uses
 * `catchExceptions = true`.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution if it is greater than zero.
 * - Before each iteration, [predicate] is evaluated.
 * - If [predicate] returns `true`, [block] is executed.
 * - After execution, waits for [delay] before the next iteration.
 * - Stops when [predicate] returns `false`, when [predicate] throws a caught exception,
 *   or when the coroutine is cancelled.
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 * - If [block] throws a [CancellationException] while the job is still active, the exception
 *   is treated as a self-cancellation attempt, logged as a warning, and ignored.
 *
 * Error handling:
 * - Non-cancellation exceptions thrown by [block] are logged and the next execution is still attempted.
 * - Non-cancellation exceptions thrown by [predicate] are logged and the loop stops, because
 *   the continuation condition could not be evaluated successfully.
 *
 * Logging:
 * - The logger is resolved before the coroutine is launched.
 * - The caller class is used as logger when available.
 * - If the caller class cannot be resolved, `runUntil` is used as fallback logger name.
 *
 * @param delay the delay between executions; must not be negative
 * @param initialDelay the delay before the first execution; must not be negative
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
    block: suspend CoroutineScope.() -> Unit,
): Job = runUntil(
    delay = delay,
    initialDelay = initialDelay,
    catchExceptions = true,
    predicate = predicate,
    taskName = null,
    block = block,
)


/**
 * Starts a coroutine that repeatedly executes [block] with a fixed [delay] between executions
 * as long as [predicate] returns `true`.
 *
 * This overload allows controlling exception handling explicitly. It exists separately because
 * version-overloaded parameters do not work reliably with this two-lambda call shape.
 *
 * Execution behavior:
 * - Waits for [initialDelay] before the first execution if it is greater than zero.
 * - Before each iteration, [predicate] is evaluated.
 * - If [predicate] returns `true`, [block] is executed.
 * - After execution, waits for [delay] before the next iteration.
 * - Stops when [predicate] returns `false`, when [predicate] throws a caught exception,
 *   or when the coroutine is cancelled.
 *
 * Cancellation:
 * - The returned [Job] is cancelled when the enclosing [CoroutineScope] is cancelled.
 * - The loop checks for cancellation via [isActive] and [ensureActive].
 * - If [block] throws a [CancellationException] while the job is still active, the exception
 *   is treated as a self-cancellation attempt, logged as a warning, and ignored.
 *
 * Error handling:
 * - If [catchExceptions] is `true`, non-cancellation exceptions thrown by [block] are logged
 *   and the next execution is still attempted.
 * - If [catchExceptions] is `true`, non-cancellation exceptions thrown by [predicate] are logged
 *   and the loop stops, because the continuation condition could not be evaluated successfully.
 * - If [catchExceptions] is `false`, non-cancellation exceptions thrown by [block] or [predicate]
 *   are rethrown and the coroutine fails.
 *
 * Logging:
 * - The logger is resolved before the coroutine is launched.
 * - The caller class is used as logger when available.
 * - If the caller class cannot be resolved, `runUntil` is used as fallback logger name.
 *
 * @param delay the delay between executions; must not be negative
 * @param initialDelay the delay before the first execution; must not be negative
 * @param catchExceptions whether non-cancellation exceptions should be logged and swallowed
 * @param predicate condition that controls whether execution should continue
 * @param taskName optional task name used for logging
 * @param block the suspending block to execute repeatedly
 *
 * @return the [Job] representing the running coroutine
 *
 * @throws IllegalArgumentException if [delay] or [initialDelay] is negative
 */
fun CoroutineScope.runUntil(
    delay: Duration,
    initialDelay: Duration = Duration.ZERO,
    catchExceptions: Boolean = true,
    predicate: suspend () -> Boolean,
    taskName: String? = null,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    require(delay >= Duration.ZERO) { "delay must not be negative" }
    require(initialDelay >= Duration.ZERO) { "initialDelay must not be negative" }

    val logging = createTaskLogging(
        defaultName = "runUntil",
        taskName = taskName,
    )

    return launch {
        if (initialDelay.isPositive()) {
            delay(initialDelay)
            ensureActive()
        }

        while (isActive) {
            val shouldRun = try {
                predicate()
            } catch (t: Throwable) {
                handleTaskException(
                    throwable = t,
                    logging = logging,
                    catchExceptions = catchExceptions,
                )

                false
            }

            ensureActive()

            if (!shouldRun) {
                break
            }

            try {
                block()
            } catch (t: Throwable) {
                handleTaskException(
                    throwable = t,
                    logging = logging,
                    catchExceptions = catchExceptions,
                )
            }

            ensureActive()
            delay(delay)
        }
    }
}