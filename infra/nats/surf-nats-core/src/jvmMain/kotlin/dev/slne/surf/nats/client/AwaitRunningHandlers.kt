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
 * Returns `null` when all of them completed, otherwise the number that is still active. The number can be 0, because
 * a handler that is being cancelled is awaited but not counted. The handlers are the direct children of the scope's
 * job at the time of the call; handlers started later are not awaited, and none is cancelled. A [deadline] that has
 * already passed counts as missed without waiting, unless no handler is running.
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

    return if (finished) null else handlers.count { it.isActive }
}
