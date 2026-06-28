package dev.slne.surf.api.core.actionbar

import dev.slne.surf.api.core.messages.adventure.uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeSource

object ActionbarService {
    private val actionbars = ConcurrentHashMap<UUID, MutableMap<UUID, Job>>()

    internal fun sendActionbar(
        scope: CoroutineScope,
        audience: Audience,
        duration: Duration,
        interval: Duration,
        fadeOut: Boolean,
        supplier: () -> Component,
        onCount: (() -> Unit)? = null,
        onFinish: (() -> Unit)? = null
    ): UUID {
        val id = UUID.randomUUID()
        val audienceUuid = audience.uuid()

        val job = scope.launch {
            try {
                val end = if (duration == Duration.INFINITE) {
                    null
                } else {
                    TimeSource.Monotonic.markNow() + duration
                }

                while (end == null || !end.hasPassedNow()) {
                    audience.sendActionBar(supplier())
                    onCount?.invoke()
                    delay(interval)
                }

                if (!fadeOut) {
                    audience.sendActionBar(Component.empty())
                }
            } finally {
                actionbars[audienceUuid]?.let { jobs ->
                    jobs.remove(id)
                    onFinish?.invoke()

                    if (jobs.isEmpty()) {
                        actionbars.remove(audienceUuid)
                    }
                }
            }
        }

        actionbars.computeIfAbsent(audienceUuid) { ConcurrentHashMap() }[id] = job
        return id
    }

    fun cancel(jobId: UUID) {
        actionbars.forEach { (_, jobs) ->
            jobs[jobId]?.cancel()
            jobs.remove(jobId)
        }
    }

    fun cancelAll() {
        actionbars.forEach { (_, jobs) ->
            jobs.values.forEach { it.cancel() }
        }
        actionbars.clear()
    }
}