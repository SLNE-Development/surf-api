package dev.slne.surf.api.spring.scheduling

import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import kotlin.coroutines.CoroutineContext

/**
 * A Spring [TaskScheduler] that runs tasks as coroutines of a context's [PluginScheduling],
 * dispatched with [dispatcher].
 *
 * Every task stops when the context's scope is cancelled. Runs of one task never overlap; a fixed
 * rate skips the starts a long run missed instead of catching up. A failing run is logged and the
 * task keeps its schedule, including a one-time task.
 *
 * @property dispatcher the dispatcher of the tasks; empty for the dispatcher of the context's
 * scope
 */
internal class CoroutineTaskScheduler(
    private val scheduling: PluginScheduling,
    val dispatcher: CoroutineContext,
) : TaskScheduler {

    override fun getClock(): Clock = scheduling.clock

    override fun schedule(task: Runnable, trigger: Trigger): ScheduledFuture<*> {
        return launch(task, TaskSchedule.Triggered(trigger))
    }

    override fun schedule(task: Runnable, startTime: Instant): ScheduledFuture<*> {
        return launch(task, TaskSchedule.Once(delayUntil(startTime)))
    }

    override fun scheduleAtFixedRate(
        task: Runnable,
        startTime: Instant,
        period: Duration,
    ): ScheduledFuture<*> {
        return launch(task, TaskSchedule.FixedRate(delayUntil(startTime), period))
    }

    override fun scheduleAtFixedRate(task: Runnable, period: Duration): ScheduledFuture<*> {
        return launch(task, TaskSchedule.FixedRate(Duration.ZERO, period))
    }

    override fun scheduleWithFixedDelay(
        task: Runnable,
        startTime: Instant,
        delay: Duration,
    ): ScheduledFuture<*> {
        return launch(task, TaskSchedule.FixedDelay(delayUntil(startTime), delay))
    }

    override fun scheduleWithFixedDelay(task: Runnable, delay: Duration): ScheduledFuture<*> {
        return launch(task, TaskSchedule.FixedDelay(Duration.ZERO, delay))
    }

    /**
     * Starts the suspending [task] on [schedule].
     *
     * @param name names the task in the log line of a failing run
     */
    fun launch(schedule: TaskSchedule, name: String, task: suspend () -> Unit): ScheduledJob {
        return scheduling.launch(dispatcher, schedule, name, task)
    }

    private fun launch(task: Runnable, schedule: TaskSchedule): ScheduledJob {
        return launch(schedule, task.toString()) { task.run() }
    }

    private fun delayUntil(startTime: Instant): Duration {
        return Duration.between(scheduling.clock.instant(), startTime)
    }
}
