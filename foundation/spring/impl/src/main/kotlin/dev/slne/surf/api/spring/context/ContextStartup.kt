package dev.slne.surf.api.spring.context

import org.springframework.core.metrics.ApplicationStartup
import org.springframework.core.metrics.jfr.FlightRecorderApplicationStartup
import java.util.*
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Supplies the [ApplicationStartup] of every context surf-api builds, import contexts included.
 *
 * With the system property [JFR_PROPERTY] set to `true`, all contexts share one
 * [FlightRecorderApplicationStartup], so Spring's own startup steps and surf-api's [StartupPhase]
 * steps land in the same JFR recording with consistent parent ids. Otherwise it supplies
 * [ApplicationStartup.DEFAULT], whose steps record nothing.
 */
internal object ContextStartup {

    /**
     * The system property that turns on Java Flight Recorder startup steps.
     */
    const val JFR_PROPERTY = "surf.startup.jfr"

    private val flightRecorder by lazy(::FlightRecorderApplicationStartup)

    /**
     * Replaces the property lookup in tests; `null` restores it.
     */
    @Volatile
    var override: ApplicationStartup? = null

    /**
     * The startup to install in a new context; reads [JFR_PROPERTY] on every call.
     */
    fun current(): ApplicationStartup {
        override?.let { return it }
        val jfrEnabled = System.getProperty(JFR_PROPERTY).toBoolean()

        return if (jfrEnabled) flightRecorder else ApplicationStartup.DEFAULT
    }
}

/**
 * A phase surf-api measures for each context, with the name of its startup step.
 */
internal enum class StartupPhase(val stepName: String, val label: String) {
    BUILD("surf.context.build", "build"),
    BOOTSTRAP("surf.plugin.bootstrap", "bootstrap"),
    LOAD("surf.plugin.load", "load"),
    ENABLE("surf.plugin.enable", "enable"),
    DISABLE("surf.plugin.disable", "disable"),
}

/**
 * The time each [StartupPhase] of one context took, summed over repeated runs of a phase.
 *
 * Thread-safe.
 */
internal class PhaseTimings {

    private val durations = EnumMap<StartupPhase, Duration>(StartupPhase::class.java)

    /**
     * Adds [duration] to the time recorded for [phase].
     */
    fun add(phase: StartupPhase, duration: Duration): Duration? {
        return synchronized(durations) {
            durations.merge(phase, duration, Duration::plus)
        }
    }

    /**
     * A snapshot of the recorded durations, in phase order.
     */
    fun snapshot(): Map<StartupPhase, Duration> = synchronized(durations) { EnumMap(durations) }
}

/**
 * Runs [block] as the startup step of [phase] tagged with [tagKey]=[tagValue], and adds its
 * duration to [timings].
 *
 * The step ends and the duration is recorded even if [block] fails, so an open step never becomes
 * the parent of unrelated later steps.
 */
internal inline fun <T> ApplicationStartup.measure(
    phase: StartupPhase,
    tagKey: String,
    tagValue: String,
    timings: PhaseTimings,
    block: () -> T,
): T {
    val step = start(phase.stepName).tag(tagKey, tagValue)
    val started = TimeSource.Monotonic.markNow()

    try {
        return block()
    } finally {
        timings.add(phase, started.elapsedNow())
        step.end()
    }
}

/**
 * Runs [block] as the startup step of the plugin phase [phase] of the plugin named [owner].
 */
internal inline fun <T> ManagedContext.measure(
    phase: StartupPhase,
    owner: String,
    block: () -> T,
): T = context.applicationStartup.measure(phase, PLUGIN_TAG, owner, timings, block)

/**
 * Describes the startup of [subject] as one log line, such as
 * `Plugin 'shop' started: build 12 ms, bootstrap 0 ms, load 3 ms, enable 4 ms`.
 *
 * The durations of all [timings] are summed per phase; phases without a recorded duration are left
 * out.
 *
 * @param subject what started, such as `Plugin 'shop'` or `Root context of 'surf-api'`
 */
internal fun describeStartup(subject: String, vararg timings: PhaseTimings): String {
    val totals = EnumMap<StartupPhase, Duration>(StartupPhase::class.java)

    for (phaseTimings in timings) {
        for ((phase, duration) in phaseTimings.snapshot()) {
            totals.merge(phase, duration, Duration::plus)
        }
    }

    return totals.entries.joinToString(prefix = "$subject started: ") { (phase, duration) ->
        "${phase.label} ${duration.inWholeMilliseconds} ms"
    }
}

/**
 * The tag that names the plugin on a plugin phase step.
 */
internal const val PLUGIN_TAG = "plugin"

/**
 * The tag that names the context on a [StartupPhase.BUILD] step.
 */
internal const val DISPLAY_NAME_TAG = "displayName"
