package dev.slne.surf.api.minestom.coroutine

import dev.slne.surf.api.minestom.extension.SchedulerManager
import kotlinx.coroutines.*
import net.minestom.server.entity.Entity
import net.minestom.server.timer.TaskSchedule
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs [block] on the entity's own scheduler at the next tick and resumes with its result.
 *
 * The scheduler of an entity is only processed while the entity is in an instance and not removed.
 * If the entity is removed before [block] ran, [block] never runs and the call is cancelled with a
 * [CancellationException]; the removal is detected on the next ticks of the global scheduler.
 *
 * @throws IllegalStateException if the entity is removed or not in an instance when called
 * @throws CancellationException if the entity is removed before [block] ran
 */
suspend fun <T : Entity, R> T.withEntity(block: (T) -> R): R {
    check(!isRemoved) { "Cannot run on a removed entity: $this" }
    checkNotNull(instance) { "Cannot run on an entity without an instance: $this" }

    return suspendCancellableCoroutine { continuation ->
        val entity = this
        val claimed = AtomicBoolean(false)

        val removalWatch = SchedulerManager.scheduleTask(
            {
                if (entity.isRemoved && claimed.compareAndSet(false, true)) {
                    continuation.cancel(CancellationException("Entity was removed before the task ran: $entity"))
                }
            },
            TaskSchedule.tick(1),
            TaskSchedule.tick(1),
        )
        val entityTask = entity.scheduler().scheduleNextTick {
            if (!claimed.compareAndSet(false, true)) return@scheduleNextTick

            removalWatch.cancel()
            try {
                continuation.resume(block(entity))
            } catch (throwable: Throwable) {
                continuation.resumeWithException(throwable)
            }
        }
        continuation.invokeOnCancellation {
            removalWatch.cancel()
            entityTask.cancel()
        }
    }
}

/**
 * Runs [block] for every entity on its own scheduler, see [withEntity], and collects the results in
 * the order of the entities.
 *
 * @throws IllegalStateException if an entity is removed or not in an instance when called
 * @throws CancellationException if an entity is removed before its task ran
 */
suspend fun <T : Entity, R> Collection<T>.withEntities(block: (T) -> R): List<R> =
    coroutineScope {
        map { entity -> async { entity.withEntity(block) } }.awaitAll()
    }
