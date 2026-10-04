package dev.slne.surf.api.minestom.coroutine

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minestom.server.entity.Entity
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T : Entity, R> T.withEntity(block: (T) -> R): R =
    suspendCancellableCoroutine { continuation ->
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


suspend fun <T : Entity, R> Collection<T>.withEntities(block: (T) -> R): List<R> =
    coroutineScope {
        map { entity -> async { entity.withEntity(block) } }.awaitAll()
    }
