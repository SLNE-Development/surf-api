package dev.slne.surf.api.minestom.coroutine

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minestom.server.entity.Entity
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs [block] on the entity's own scheduler at the next tick and resumes with its result.
 *
 * The scheduler of an entity is only processed while the entity is in an instance and not removed.
 *
 * @throws IllegalStateException if the entity is removed or not in an instance when called
 */
suspend fun <T : Entity, R> T.withEntity(block: (T) -> R): R {
    check(!isRemoved) { "Cannot run on a removed entity: $this" }
    checkNotNull(instance) { "Cannot run on an entity without an instance: $this" }

    return suspendCancellableCoroutine { continuation ->
        val entity = this
        val task = scheduler().scheduleNextTick {
            try {
                continuation.resume(block(entity))
            } catch (throwable: Throwable) {
                continuation.resumeWithException(throwable)
            }
        }
        continuation.invokeOnCancellation { task.cancel() }
    }
}

/**
 * Runs [block] for every entity on its own scheduler, see [withEntity], and collects the results in
 * the order of the entities.
 *
 * @throws IllegalStateException if an entity is removed or not in an instance when called
 */
suspend fun <T : Entity, R> Collection<T>.withEntities(block: (T) -> R): List<R> =
    coroutineScope {
        map { entity -> async { entity.withEntity(block) } }.awaitAll()
    }
