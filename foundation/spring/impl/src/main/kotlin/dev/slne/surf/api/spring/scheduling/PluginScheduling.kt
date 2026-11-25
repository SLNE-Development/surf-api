package dev.slne.surf.api.spring.scheduling

import kotlinx.coroutines.*
import org.apache.commons.logging.LogFactory
import org.springframework.scheduling.support.SimpleTriggerContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Runs the scheduled tasks of one context inside the context's coroutine scope.
 *
 * Every task is a coroutine below one [SupervisorJob], a child of the context's scope, so
 * cancelling that scope, [stop] or [cancel] ends every task, and a failing task ends no other. A
 * failing run is logged under [owner] and the task keeps its schedule. Run times come from
 * [clock], waiting from `delay`, so a test dispatcher with a matching clock runs schedules in
 * virtual time.
 *
 * @property owner the name of the plugin or application the tasks belong to
 */
internal class PluginScheduling(
    val owner: String,
    contextScope: CoroutineScope,
    val clock: Clock,
) {

    private val log = LogFactory.getLog(PluginScheduling::class.java)
    private val job = SupervisorJob(contextScope.coroutineContext.job)
    private val scope = CoroutineScope(contextScope.coroutineContext + job)

    /**
     * Starts [task] on [schedule], dispatched with [context].
     *
     * @param name names the task in the log line of a failing run
     */
    fun launch(
        context: CoroutineContext,
        schedule: TaskSchedule,
        name: String,
        task: suspend () -> Unit,
    ): ScheduledJob {
        val nextRun = NextRun(clock)
        val taskJob = scope.launch(context) {
            ScheduleRunner(clock, nextRun) { runLogging(name, task) }.run(schedule)
        }

        return ScheduledJob(taskJob, nextRun)
    }

    /**
     * Cancels every task and waits up to [STOP_TIMEOUT] for running ones to finish.
     */
    suspend fun stop() {
        job.cancel()
        val stopped = withTimeoutOrNull(STOP_TIMEOUT) { job.join() }
        if (stopped == null) {
            log.warn("Scheduled tasks of '$owner' still run after $STOP_TIMEOUT")
        }
    }

    /**
     * Cancels every task without waiting.
     */
    fun cancel() {
        job.cancel()
    }

    private suspend fun runLogging(name: String, task: suspend () -> Unit) {
        try {
            task()
        } catch (exception: Throwable) {
            currentCoroutineContext().ensureActive()
            log.error("Scheduled task $name of '$owner' failed", exception)
        }
    }

    private companion object {
        val STOP_TIMEOUT = 5.seconds
    }
}

/**
 * Waits for and performs the runs of one task.
 */
private class ScheduleRunner(
    private val clock: Clock,
    private val nextRun: NextRun,
    private val runOnce: suspend () -> Unit,
) {

    suspend fun run(schedule: TaskSchedule) {
        when (schedule) {
            is TaskSchedule.Once -> {
                awaitAfter(schedule.delay)
                runOnce()
            }

            is TaskSchedule.FixedDelay -> runWithFixedDelay(schedule)
            is TaskSchedule.FixedRate -> runAtFixedRate(schedule)
            is TaskSchedule.Triggered -> runTriggered(schedule)
        }
    }

    private suspend fun runWithFixedDelay(schedule: TaskSchedule.FixedDelay) {
        awaitAfter(schedule.initialDelay)

        while (true) {
            runOnce()
            awaitAfter(schedule.delay)
        }
    }

    private suspend fun runAtFixedRate(schedule: TaskSchedule.FixedRate) {
        var start = clock.instant().plus(schedule.initialDelay)

        while (true) {
            awaitUntil(start)
            runOnce()
            start = nextRateStart(start, schedule.period)
        }
    }

    private suspend fun runTriggered(schedule: TaskSchedule.Triggered) {
        val triggerContext = SimpleTriggerContext(clock)

        while (true) {
            val scheduled = schedule.trigger.nextExecution(triggerContext) ?: return
            awaitUntil(scheduled)

            val started = clock.instant()
            runOnce()
            triggerContext.update(scheduled, started, clock.instant())
        }
    }

    /**
     * The first start of the rate after now: `previous + n * period` for the smallest `n >= 1`
     * that lies in the future, which skips every start a long run missed.
     */
    private fun nextRateStart(previous: Instant, period: Duration): Instant {
        val elapsed = Duration.between(previous, clock.instant())
        val periods = elapsed.toNanos() / period.toNanos() + 1

        return previous.plus(period.multipliedBy(periods.coerceAtLeast(1)))
    }

    private suspend fun awaitAfter(delay: Duration) {
        awaitUntil(clock.instant().plus(delay))
    }

    private suspend fun awaitUntil(time: Instant) {
        nextRun.time = time
        delay(Duration.between(clock.instant(), time).toMillis().milliseconds)
    }
}

/**
 * The start of a task's next run, as far as it is known.
 */
internal class NextRun(private val clock: Clock) {

    @Volatile
    var time: Instant? = null

    fun delayNanos(): Long {
        val next = time ?: return 0

        return Duration.between(clock.instant(), next).toNanos()
    }
}

/**
 * The [ScheduledFuture] of a task that [PluginScheduling] runs; cancelling it cancels the task's
 * coroutine.
 *
 * The future completes when the task ends: after its only run, when its trigger has no further
 * run, or when it is cancelled. It never carries a result.
 */
internal class ScheduledJob(
    private val job: Job,
    private val nextRun: NextRun,
) : ScheduledFuture<Any?> {

    override fun getDelay(unit: TimeUnit): Long {
        return unit.convert(nextRun.delayNanos(), TimeUnit.NANOSECONDS)
    }

    override fun compareTo(other: Delayed): Int {
        return getDelay(TimeUnit.NANOSECONDS).compareTo(other.getDelay(TimeUnit.NANOSECONDS))
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (job.isCompleted) return false

        job.cancel()

        return true
    }

    override fun isCancelled(): Boolean = job.isCancelled

    override fun isDone(): Boolean = job.isCompleted

    override fun get(): Any? {
        runBlocking { job.join() }

        return result()
    }

    override fun get(timeout: Long, unit: TimeUnit): Any? {
        val completed = runBlocking {
            withTimeoutOrNull(unit.toMillis(timeout).milliseconds) { job.join() }
        }
        if (completed == null) throw TimeoutException("The scheduled task did not end in time")

        return result()
    }

    private fun result(): Any? {
        if (job.isCancelled) throw CancellationException("The scheduled task was cancelled")

        return null
    }
}
