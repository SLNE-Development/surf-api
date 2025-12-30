package dev.slne.surf.redis.sync.map

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.map.SyncMap.Companion.DEFAULT_TTL
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import org.redisson.client.codec.StringCodec
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

    private val dataBucket = api.redissonReactive.getBucket<String>(dataKey, StringCodec.INSTANCE)
    private val remoteVersion = api.redissonReactive.getAtomicLong(verKey)

    @Volatile
    private var localVersion: Long = 0L

    companion object {
        /**
         * Default TTL for snapshot and version keys.
         *
         * Refreshed periodically by the heartbeat while at least one node is alive.
         */
        val DEFAULT_TTL = 5.minutes
    }

    override suspend fun init() {
        super.init()
        startHeartbeat()
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

        scope.launch {
            val keyJson = api.json.encodeToString(keySerializer, key)
            val valueJson = api.json.encodeToString(valueSerializer, value)
            publishLocalDelta(Delta.Put(keyJson, valueJson))
        }

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

        scope.launch {
            val keyJson = api.json.encodeToString(keySerializer, key)
            publishLocalDelta(Delta.Remove(keyJson))
        }

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

        scope.launch {
            publishLocalDelta(Delta.Clear)
        }

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

    override suspend fun loadSnapshot() {
        val snapshotJson = dataBucket.get().awaitSingleOrNull()
        val version = remoteVersion.get().awaitSingle()

        val loaded =
            if (snapshotJson.isNullOrBlank()) emptyMap()
            else api.json.decodeFromString(snapshotSerializer, snapshotJson)

        lock.write {
            map.clear()
            map.putAll(loaded)
        }

        localVersion = version
    }

    override fun handleIncoming(message: String) {
        val envelope = api.json.decodeFromString<Envelope>(message)

        val current = localVersion
        when {
            envelope.version == current + 1 -> {
                applyDelta(envelope.delta)
                localVersion = envelope.version
            }

            envelope.version <= current -> {
                // duplicate / out-of-order -> ignore
            }

            else -> {
                scope.launch { loadSnapshot() }
            }
        }
    }

    /**
     * Publishes a local delta:
     * - increments the global version,
     * - persists the snapshot with TTL,
     * - broadcasts the delta via Pub/Sub.
     */
    private suspend fun publishLocalDelta(delta: Delta) {
        val newVersion = remoteVersion.incrementAndGet().awaitSingle()
        localVersion = newVersion

        persistSnapshot(newVersion)

        val msg = api.json.encodeToString(Envelope(newVersion, delta))
        topic.publish(msg).awaitSingle()
    }

    /**
     * Persists the full map snapshot and version with TTL.
     *
     * Used for late joiners and recovery after missed deltas.
     */
    private suspend fun persistSnapshot(version: Long) {
        val snapshotJson = lock.read { api.json.encodeToString(snapshotSerializer, map) }


        dataBucket.set(snapshotJson, ttl.toJavaDuration()).awaitSingle()
        remoteVersion.set(version).awaitSingle()
        remoteVersion.expire(ttl.toJavaDuration()).awaitSingle()
    }

    /**
     * Periodically refreshes TTLs for snapshot and version keys.
     *
     * Ensures the map remains ephemeral and is automatically
     * removed from Redis if all nodes go offline.
     */
    private fun startHeartbeat() {
        if (ttl == Duration.ZERO || ttl.isNegative()) return

        scope.launch {
            while (isActive) {
                delay(ttl / 2)
                try {
                    persistSnapshot(localVersion)
                } catch (_: Throwable) {
                }
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
