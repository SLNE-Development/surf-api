package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.Blocking
import kotlin.time.Duration
import kotlin.time.TimeMark

/**
 * Blocks until the handlers running in [handlerScope] have completed or [deadline] passes.
 *
 * Returns `null` when all of them completed, otherwise the positive number that has not completed yet, including
 * handlers that are being cancelled. The handlers are the direct children of the scope's job at the time of the call.
 * Handlers started later are not awaited, and none is cancelled. A [deadline] that has already passed counts as
 * missed without waiting, unless no handler is running.
 *
 * @throws InterruptedException when the calling thread is interrupted while it waits
 */
@InternalNatsApi
@Blocking
internal fun awaitRunningHandlers(handlerScope: CoroutineScope, deadline: TimeMark): Int? {
    val handlers = handlerScope.coroutineContext.job.children.toList()
    if (handlers.isEmpty()) return null

    val finished = runBlocking {
        val timeLeft = (-deadline.elapsedNow()).coerceAtLeast(Duration.ZERO)
        withTimeoutOrNull(timeLeft) { handlers.joinAll() } != null
    }

    if (finished) return null
    return handlers.count { !it.isCompleted }.takeIf { it > 0 }
}
