package dev.slne.surf.redis.sync

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.redis.util.DisposableAware
import dev.slne.surf.surfapi.core.api.util.logger
import org.jetbrains.annotations.MustBeInvokedByOverriders
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.min
import kotlin.time.Duration


abstract class AbstractSyncStructure<L, R : AbstractSyncStructure.VersionedSnapshot>(
    protected val api: RedisApi,
    id: String,
    override val ttl: Duration
) : DisposableAware(), SyncStructure<L> {
    companion object {
        const val NAMESPACE = "surf-redis:sync:"
        private val log = logger()
    }

    override val id = id.replace(":", "_")

    private val listeners = CopyOnWriteArrayList<(L) -> Unit>()
    protected val lock = ReentrantReadWriteLock()

    private val listenerIds = ConcurrentHashMap.newKeySet<Int>()

    @MustBeInvokedByOverriders
    override fun init(): Mono<Void> {
        return registerListeners()
            .then(loadFromRemote())
            .then(refreshTtl())
            .doOnSuccess { trackDisposable(startTtlRefresh().subscribeOn(RedisInstance.get().ttlRefreshScheduler).subscribe()) }
            .then()
    }

    private fun registerListeners(): Mono<Void> = Flux.merge(registerListeners0())
        .doOnError { e ->
            log.atSevere()
                .withCause(e)
                .log("Failed to register listeners for $id")
        }
        .doOnNext { listenerIds.add(it) }
        .then()

    private fun unregisterListeners(): Mono<Void> = Flux.fromIterable(listenerIds)
        .concatMap { unregisterListener(it).thenReturn(it) }
        .doOnError { e ->
            log.atSevere()
                .withCause(e)
                .log("Failed to unregister listeners for $id")
        }
        .doOnNext { listenerIds.remove(it) }
        .then()

    protected abstract fun registerListeners0(): List<Mono<Int>>
    protected abstract fun unregisterListener(id: Int): Mono<*>

    @MustBeInvokedByOverriders
    override fun dispose0() {
        unregisterListeners().subscribe()
    }

    override fun addListener(listener: (L) -> Unit) {
        listeners += listener
    }

    override fun removeListener(listener: (L) -> Unit) {
        listeners -= listener
    }

    protected fun notifyListeners(value: L) {
        for (listener in listeners) {
            try {
                listener(value)
            } catch (e: Throwable) {
                log.atSevere()
                    .withCause(e)
                    .log("Error notifying listener for $id")
            }
        }
    }

    protected fun loadFromRemote(): Mono<Void> = loadFromRemote0()
        .onErrorResume {
            log.atWarning()
                .withCause(it)
                .log("Failed to load remote state for $id")
            Mono.empty()
        }
        .doOnSuccess { raw ->
            if (raw != null) {
                overrideFromRemote(raw)
            }
        }
        .then()

    protected abstract fun loadFromRemote0(): Mono<R>
    protected abstract fun overrideFromRemote(raw: R)

    private fun startTtlRefresh(): Flux<Void> {
        if (ttl == Duration.ZERO || ttl.isNegative()) return Flux.empty()
        val period = java.time.Duration.ofSeconds(min(ttl.inWholeSeconds - 2, 5))

        return Flux.interval(period, RedisInstance.get().ttlRefreshScheduler)
            .concatMap {
                refreshTtl()
                    .then()
                    .onErrorResume { e ->
                        log.atSevere()
                            .withCause(e)
                            .log("Failed to refresh TTL for $id")
                        Mono.empty()
                    }
            }
    }

    protected abstract fun refreshTtl(): Mono<*>

    interface VersionedSnapshot {
        val version: Long
    }

    data class SimpleVersionedSnapshot<V>(
        val value: V,
        override val version: Long
    ) : VersionedSnapshot {
        companion object {
            fun <V : Any> fromTuple(tuple: Tuple2<V, Long>) = SimpleVersionedSnapshot(tuple.t1, tuple.t2)
        }
    }
}