package dev.slne.surf.redis.sync.map

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.map.SyncMap.Companion.DEFAULT_TTL
import dev.slne.surf.redis.util.component1
import dev.slne.surf.redis.util.component2
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Replicated in-memory map that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds a local `Map<K, V>` instance.
 * - **Delta-based replication:** mutations publish deltas via Redis Pub/Sub.
 * - **Snapshot for late joiners:** the full map is stored in Redis with a TTL.
 * - **Versioning:** every mutation increments a global version counter.
 * - **Resync:** if a node detects a version gap, it reloads the snapshot.
 * - **Ephemeral storage:** snapshot and version keys expire automatically and
 *   are refreshed via heartbeat while nodes are alive.
 *
 * ## Consistency model
 * - Eventual consistency.
 * - Deltas are applied only if `version == localVersion + 1`.
 * - Duplicate or out-of-order deltas are ignored.
 * - Missing deltas trigger a snapshot reload.
 *
 * ## Threading / API behavior
 * - Public mutating methods are **non-suspending** and never block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread applying the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 *
 * ## Failure semantics
 * - If all nodes go offline, Redis state expires automatically after [ttl].
 * - The next node recreates the map from the provided operations.
 *
 * @param api owning [RedisApi] instance
 * @param id unique identifier for this map (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis operations
 * @param keySerializer serializer for map keys
 * @param valueSerializer serializer for map values
 * @param ttl TTL for snapshot and version keys (default: [DEFAULT_TTL])
 */
class SyncMap<K : Any, V : Any> internal constructor(
    api: RedisApi,
    id: String,
    scope: CoroutineScope,
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
    private val ttl: Duration = DEFAULT_TTL,
) : SyncStructure<V>(api, id, scope) {

    private val map = Object2ObjectOpenHashMap<K, V>()
    private val listeners = CopyOnWriteArrayList<(SyncMapChange) -> Unit>()

    private val snapshotSerializer = MapSerializer(keySerializer, valueSerializer)

    private val dataKey = "surf-redis:sync:map:$id:snapshot"
    private val verKey = "surf-redis:sync:map:$id:ver"
    override val redisChannel: String = "surf-redis:sync:map:$id"

    private val dataBucket by lazy { api.redissonReactive.getBucket<String>(dataKey, StringCodec.INSTANCE) }
    private val remoteVersion by lazy { api.redissonReactive.getAtomicLong(verKey) }

    @Volatile
    private var localVersion: Long = 0L

    companion object {
        private val log = logger()

        /**
         * Default TTL for snapshot and version keys.
         *
         * Refreshed periodically by the heartbeat while at least one node is alive.
         */
        val DEFAULT_TTL = 5.minutes
    }

    override fun setupSubscription(): Flux<Void> {
        return super.setupSubscription()
            .mergeWith(startHeartbeat())
    }

    /**
     * Returns a copy of the current map state.
     *
     * The returned map is a snapshot and will not reflect future updates.
     */
    fun snapshot(): Object2ObjectOpenHashMap<K, V> = lock.read { Object2ObjectOpenHashMap(map) }

    /** @return number of entries in the map */
    fun size(): Int = lock.read { map.size }

    /** @return `true` if the map is empty */
    fun isEmpty(): Boolean = lock.read { map.isEmpty() }

    /** @return value associated with [key], or `null` if absent */
    fun get(key: K): V? = lock.read { map[key] }

    /** @return `true` if the map contains [key] */
    fun containsKey(key: K): Boolean = lock.read { map.containsKey(key) }

    /**
     * Associates [value] with [key] in the map and replicates the change.
     *
     * The local map is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the previous value associated with [key], or `null` if there was no mapping
     */
    fun put(key: K, value: V): V? {
        val old = lock.write { map.put(key, value) }

        val keyJson = api.json.encodeToString(keySerializer, key)
        val valueJson = api.json.encodeToString(valueSerializer, value)
        publishLocalDelta(Delta.Put(keyJson, valueJson)).subscribe()

        notifyListeners(listeners, SyncMapChange.Put(key, value, old))

        return old
    }

    /**
     * Removes the mapping for [key] from the map and replicates the change.
     *
     * The local map is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the value that was associated with [key], or `null` if there was no mapping
     */
    fun remove(key: K): V? {
        val removed = lock.write { map.remove(key) } ?: return null

        val keyJson = api.json.encodeToString(keySerializer, key)
        publishLocalDelta(Delta.Remove(keyJson)).subscribe()

        notifyListeners(listeners, SyncMapChange.Removed(key, removed))

        return removed
    }

    /**
     * Removes all mappings from the map and replicates the change.
     *
     * The local map is cleared immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * If the map is already empty, this method is a no-op and does not publish a delta.
     */
    fun clear() {
        val hadElements = lock.write {
            val had = map.isNotEmpty()
            map.clear()
            had
        }
        if (!hadElements) return

        publishLocalDelta(Delta.Clear).subscribe()

        notifyListeners(listeners, SyncMapChange.Cleared)
    }

    /**
     * Registers a change listener.
     *
     * The listener is invoked for all local and remote changes.
     */
    fun addListener(listener: (SyncMapChange) -> Unit) {
        listeners += listener
    }

    /** Removes a previously registered listener. */
    fun removeListener(listener: (SyncMapChange) -> Unit) {
        listeners -= listener
    }

    override fun loadSnapshot(): Mono<Void> {
        return dataBucket.get()
            .zipWith(remoteVersion.get())
            .flatMap { (snapshotJson, version) ->
                Mono.fromCallable {
                    val loaded =
                        if (snapshotJson.isNullOrBlank()) emptyMap()
                        else api.json.decodeFromString(snapshotSerializer, snapshotJson)

                    lock.write {
                        map.clear()
                        map.putAll(loaded)
                    }

                    localVersion = version
                }
            }.then()
    }

    override fun handleIncoming(message: String): Mono<Void> {
        return Mono.defer {
            val envelope = api.json.decodeFromString<Envelope>(message)
            val current = localVersion

            when {
                envelope.version == current + 1 -> {
                    Mono.fromRunnable {
                        applyDelta(envelope.delta)
                        localVersion = envelope.version
                    }
                }

                envelope.version <= current -> {
                    // duplicate / out-of-order -> ignore
                    Mono.empty()
                }

                else -> loadSnapshot()
            }
        }
    }


    /**
     * Publishes a local delta:
     * - increments the global version,
     * - persists the snapshot with TTL,
     * - broadcasts the delta via Pub/Sub.
     */
    private fun publishLocalDelta(delta: Delta): Mono<Void> {
        return remoteVersion.incrementAndGet()
            .doOnNext { localVersion = it }
            .flatMap { newVersion -> persistSnapshot(newVersion).thenReturn(newVersion) }
            .flatMap { version ->
                val msg = api.json.encodeToString(Envelope(version, delta))
                topic.publish(msg)
            }.doOnError { t ->
                log.atSevere()
                    .withCause(t)
                    .log("Failed to publish local delta for SyncMap '$id'")
            }.then()
    }

    /**
     * Persists the full map snapshot and version with TTL.
     *
     * Used for late joiners and recovery after missed deltas.
     */
    private fun persistSnapshot(version: Long): Mono<Void> {
        return Mono.fromCallable {
            lock.read { api.json.encodeToString(snapshotSerializer, map) }
        }.flatMap { snapshotJson ->
            dataBucket.set(snapshotJson, ttl.toJavaDuration())
                .then(remoteVersion.set(version))
                .then(remoteVersion.expireIfGreater(ttl.toJavaDuration()))
        }.doOnError { t ->
            log.atSevere()
                .withCause(t)
                .log("Failed to persist snapshot for SyncMap '$id'")
        }.then()
    }

    /**
     * Periodically refreshes TTLs for snapshot and version keys.
     *
     * Ensures the map remains ephemeral and is automatically
     * removed from Redis if all nodes go offline.
     */
    private fun startHeartbeat(): Flux<Void> {
        if (ttl == Duration.ZERO || ttl.isNegative()) return Flux.empty()

        return Flux.interval((ttl / 2).toJavaDuration())
            .concatMap {
                persistSnapshot(localVersion)
                    .onErrorResume { t ->
                        log.atSevere()
                            .withCause(t)
                            .log("Failed to refresh heartbeat for SyncMap '$id'")
                        Mono.empty()
                    }
            }
    }

    /**
     * Applies a single delta to the local map and notifies listeners.
     */
    private fun applyDelta(delta: Delta) {
        when (delta) {
            is Delta.Put -> {
                val key = try {
                    api.json.decodeFromString(keySerializer, delta.keyJson)
                } catch (_: Throwable) {
                    return
                }
                val value = try {
                    api.json.decodeFromString(valueSerializer, delta.valueJson)
                } catch (_: Throwable) {
                    return
                }

                val old = lock.write { map.put(key, value) }
                notifyListeners(listeners, SyncMapChange.Put(key, value, old))
            }

            is Delta.Remove -> {
                val key = try {
                    api.json.decodeFromString(keySerializer, delta.keyJson)
                } catch (_: Throwable) {
                    return
                }

                val removed = lock.write { map.remove(key) } ?: return
                notifyListeners(listeners, SyncMapChange.Removed(key, removed))
            }

            Delta.Clear -> {
                val hadElements = lock.write {
                    val had = map.isNotEmpty()
                    map.clear()
                    had
                }
                if (hadElements) notifyListeners(listeners, SyncMapChange.Cleared)
            }
        }
    }

    /**
     * Wire format for Pub/Sub messages.
     */
    @Serializable
    private data class Envelope(
        val version: Long,
        val delta: Delta
    )

    /**
     * Delta operations for map replication.
     */
    @Serializable
    private sealed interface Delta {
        @Serializable
        data class Put(val keyJson: String, val valueJson: String) : Delta

        @Serializable
        data class Remove(val keyJson: String) : Delta

        @Serializable
        data object Clear : Delta
    }
}
