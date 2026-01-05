package dev.slne.surf.redis.sync

import dev.slne.surf.redis.util.InternalRedisAPI
import reactor.core.Disposable
import reactor.core.publisher.Mono
import kotlin.time.Duration

/**
 * Base contract for replicated, in-memory synchronized structures.
 *
 * A [SyncStructure] provides:
 * - A logical [id] used for Redis keys/channels.
 * - A [ttl] used for any remote state that should expire automatically.
 * - A lifecycle hook [init] to bootstrap remote state and start background tasks.
 * - A simple listener mechanism for propagating local/remote changes to consumers.
 */
interface SyncStructure<L> : Disposable {
    val id: String
    val ttl: Duration

    @InternalRedisAPI
    fun init(): Mono<Void>

    fun addListener(listener: (L) -> Unit)
    fun removeListener(listener: (L) -> Unit)
}
