package dev.slne.surf.api.spring.scheduling

import org.springframework.format.annotation.DurationFormat
import org.springframework.format.datetime.standard.DurationFormatterUtils
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronTrigger
import org.springframework.util.StringUtils
import org.springframework.util.StringValueResolver
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * When the runs of a scheduled task start.
 *
 * No schedule lets runs of one task overlap: a run starts only after the previous one completed.
 */
internal sealed interface TaskSchedule {

    /**
     * One run after [delay].
     */
    data class Once(val delay: Duration) : TaskSchedule

    /**
     * Runs after [initialDelay], then [delay] after each run completed.
     *
     * @throws IllegalArgumentException if [delay] is not positive
     */
    data class FixedDelay(val initialDelay: Duration, val delay: Duration) : TaskSchedule {
        init {
            require(delay.isPositive) { "The fixed delay must be positive, but is $delay" }
        }
    }

    /**
     * Runs after [initialDelay], then at every multiple of [period] after the first start. A start
     * that passes while a run is still busy is skipped.
     *
     * @throws IllegalArgumentException if [period] is not positive
     */
    data class FixedRate(val initialDelay: Duration, val period: Duration) : TaskSchedule {
        init {
            require(period.isPositive) { "The fixed rate must be positive, but is $period" }
        }
    }

    /**
     * Runs whenever [trigger] says, such as a cron expression; ends when it returns `null`.
     */
    data class Triggered(val trigger: Trigger) : TaskSchedule
}

/**
 * Reads the schedule of a `@Scheduled` annotation with the rules of Spring's
 * `ScheduledAnnotationBeanPostProcessor`.
 *
 * Exactly one of `cron`, `fixedDelay` and `fixedRate` is allowed, in their plain or `String` form;
 * with none, `initialDelay` alone makes a one-time task. String attributes go through [resolver]
 * first, so they may hold placeholders.
 *
 * @return the schedule, or `null` if the cron expression is [Scheduled.CRON_DISABLED]
 * @throws IllegalArgumentException if the attributes contradict each other or do not parse
 */
internal fun Scheduled.toTaskSchedule(resolver: StringValueResolver?): TaskSchedule? {
    val reader = ScheduledReader(this, resolver)
    val initialDelay = reader.initialDelay()
    val schedules = listOfNotNull(
        reader.cron(initialDelay),
        reader.fixedDelay(initialDelay),
        reader.fixedRate(initialDelay),
    )
    require(schedules.size <= 1) {
        "Exactly one of the 'cron', 'fixedDelay' or 'fixedRate' attributes is required"
    }

    val schedule = schedules.singleOrNull()
    if (schedule != null) return schedule.taskSchedule

    require(initialDelay != null) { "One-time task only supported with specified initial delay" }

    return TaskSchedule.Once(initialDelay)
}

/**
 * A trigger attribute that is set; [taskSchedule] is `null` for a disabled cron expression.
 */
private class DeclaredSchedule(val taskSchedule: TaskSchedule?)

private class ScheduledReader(
    private val scheduled: Scheduled,
    private val resolver: StringValueResolver?,
) {

    fun initialDelay(): Duration? {
        val plain = toDuration(scheduled.initialDelay)
        val text = resolver.resolveAttribute(scheduled.initialDelayString)
        if (text == null) return plain

        require(plain == null) { "Specify 'initialDelay' or 'initialDelayString', not both" }

        return parseDuration(text, "initialDelayString")
    }

    fun cron(initialDelay: Duration?): DeclaredSchedule? {
        val expression = resolver.resolveAttribute(scheduled.cron) ?: return null
        require(initialDelay == null) { "'initialDelay' not supported for cron triggers" }

        if (expression == Scheduled.CRON_DISABLED) return DeclaredSchedule(null)

        val zone = resolver.resolveAttribute(scheduled.zone)
        val trigger = if (zone == null) {
            CronTrigger(expression)
        } else {
            CronTrigger(expression, StringUtils.parseTimeZoneString(zone))
        }

        return DeclaredSchedule(TaskSchedule.Triggered(trigger))
    }

    fun fixedDelay(initialDelay: Duration?): DeclaredSchedule? {
        val delay = interval(scheduled.fixedDelay, scheduled.fixedDelayString, "fixedDelayString")
            ?: return null

        return DeclaredSchedule(TaskSchedule.FixedDelay(initialDelay ?: Duration.ZERO, delay))
    }

    fun fixedRate(initialDelay: Duration?): DeclaredSchedule? {
        val period = interval(scheduled.fixedRate, scheduled.fixedRateString, "fixedRateString")
            ?: return null

        return DeclaredSchedule(TaskSchedule.FixedRate(initialDelay ?: Duration.ZERO, period))
    }

    /**
     * The interval of a plain attribute and its `String` form, or `null` if neither is set.
     */
    private fun interval(plain: Long, text: String, textAttribute: String): Duration? {
        val plainDuration = toDuration(plain)
        val resolved = resolver.resolveAttribute(text)
        if (resolved == null) return plainDuration

        require(plainDuration == null) {
            "Exactly one of the 'cron', 'fixedDelay' or 'fixedRate' attributes is required"
        }

        return parseDuration(resolved, textAttribute)
    }

    /**
     * [value] in the annotation's time unit, or `null` for a negative value, which marks an unset
     * attribute.
     */
    private fun toDuration(value: Long): Duration? {
        if (value < 0) return null

        return Duration.of(value, scheduled.timeUnit.toChronoUnit())
    }

    private fun parseDuration(value: String, attribute: String): Duration {
        try {
            return DurationFormatterUtils.detectAndParse(value, unitOf(scheduled.timeUnit))
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException("Invalid $attribute value \"$value\"; $exception")
        }
    }

    private fun unitOf(timeUnit: TimeUnit): DurationFormat.Unit {
        return DurationFormat.Unit.fromChronoUnit(timeUnit.toChronoUnit())
    }
}

/**
 * Resolves the placeholders in the `@Scheduled` attribute [value] as Spring does.
 *
 * @return the resolved value, or `null` if [value] is blank or resolves to `null` or an empty
 * string, which marks the attribute as unset
 */
internal fun StringValueResolver?.resolveAttribute(value: String): String? {
    if (!StringUtils.hasText(value)) return null

    val resolved = if (this == null) value else resolveStringValue(value)

    return resolved?.takeIf(String::isNotEmpty)
}
