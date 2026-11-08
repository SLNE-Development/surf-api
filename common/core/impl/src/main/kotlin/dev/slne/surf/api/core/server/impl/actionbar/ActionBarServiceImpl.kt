package dev.slne.surf.api.core.server.impl.actionbar

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.SurfApiCore
import dev.slne.surf.api.core.actionbar.ActionBarFinishReason
import dev.slne.surf.api.core.actionbar.ActionBarService
import dev.slne.surf.api.core.messages.adventure.nameOrNull
import dev.slne.surf.api.core.messages.adventure.uuidOrNull
import dev.slne.surf.api.core.server.impl.SurfApiCoreImpl
import dev.slne.surf.api.core.util.logger
import kotlinx.coroutines.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.TimeSource

@AutoService(ActionBarService::class)
class ActionBarServiceImpl : ActionBarService {

    companion object {
        private val log = logger()
    }

    override fun sendActionBar(
        scope: CoroutineScope,
        audience: Audience,
        duration: Duration,
        interval: Duration,
        fadeOut: Boolean,
        autoCancelPlayer: Boolean,
        computation: () -> Component,
        afterTick: (() -> Unit)?,
        onFinish: ((ActionBarFinishReason) -> Unit)?
    ): Job {
        require(duration.isPositive()) { "duration must be positive" }
        require(interval.isPositive()) { "interval must be positive" }

        val uuid = audience.uuidOrNull()
        val audienceName = audience.nameOrNull()
            ?: uuid?.toString()
            ?: "#Unknown"

        val autoCancelUuid = if (
            autoCancelPlayer &&
            uuid != null &&
            SurfApiCoreImpl.get().isPlayer(audience)
        ) {
            uuid
        } else {
            null
        }

        fun shouldAutoCancel(): Boolean {
            return autoCancelUuid != null && SurfApiCore.getPlayer(autoCancelUuid) == null
        }

        val finishReason = AtomicReference(ActionBarFinishReason.CANCELLED)

        val job = scope.launch {
            val end = TimeSource.Monotonic.markNow() + duration

            while (true) {
                ensureActive()

                if (shouldAutoCancel()) {
                    finishReason.set(ActionBarFinishReason.AUTO_CANCELLED)
                    break
                }

                if (end.hasPassedNow()) {
                    finishReason.set(ActionBarFinishReason.COMPLETED)
                    break
                }

                val tickStart = TimeSource.Monotonic.markNow()
                val component = try {
                    computation()
                } catch (t: Throwable) {
                    log.atWarning()
                        .withCause(t)
                        .atMostEvery(900, TimeUnit.MILLISECONDS)
                        .log("Failed to compute actionbar for $audienceName")
                    null
                }

                if (component != null) {
                    audience.sendActionBar(component)
                }

                try {
                    afterTick?.invoke()
                } catch (t: Throwable) {
                    log.atWarning()
                        .withCause(t)
                        .atMostEvery(900, TimeUnit.MILLISECONDS)
                        .log("Failed to invoke afterTick for $audienceName")
                }

                val remainingDelay = interval - tickStart.elapsedNow()
                if (remainingDelay.isPositive()) {
                    delay(remainingDelay)
                }
            }

            ensureActive()

            if (!fadeOut) {
                audience.sendActionBar(Component.empty())
            }
        }

        job.invokeOnCompletion { throwable ->
            val reason = when (throwable) {
                null -> finishReason.get()
                is CancellationException -> ActionBarFinishReason.CANCELLED
                else -> ActionBarFinishReason.FAILED
            }

            try {
                onFinish?.invoke(reason)
            } catch (throwable: Throwable) {
                log.atWarning()
                    .withCause(throwable)
                    .log("Failed to invoke onFinish for $audienceName")
            }
        }

        return job
    }
}