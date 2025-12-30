package dev.slne.surf.redis.sync.set

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.set.SyncSet.Companion.DEFAULT_TTL
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import org.redisson.client.codec.StringCodec
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Replicated in-memory set that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds a local `Set<T>` instance.
 * - **Delta-based replication:** mutations publish deltas via Redis Pub/Sub.
 * - **Snapshot for late joiners:** the full set is stored in Redis with a TTL.
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
 * - Mutating methods are **non-suspending** and do not block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread applying the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 *
 * ## Failure semantics
 * - If all nodes go offline, Redis state expires automatically after [ttl].
 * - The next node recreates the set from the provided operations.
 *
 * @param api owning [RedisApi] instance
 * @param id unique identifier for this set (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis operations
 * @param elementSerializer serializer for set elements
 * @param ttl TTL for snapshot and version keys (default: [DEFAULT_TTL])
 */
class SyncSet<T : Any> internal constructor(
    api: RedisApi,
    id: String,
    scope: CoroutineScope,
    private val elementSerializer: KSerializer<T>,
    private val ttl: Duration = DEFAULT_TTL,
) : SyncStructure<T>(api, id, scope) {

    private val set = ObjectOpenHashSet<T>()
    private val snapshotSerializer = SetSerializer(elementSerializer)

    private val listeners = CopyOnWriteArrayList<(SyncSetChange) -> Unit>()

    private val dataKey = "surf-redis:sync:set:$id:snapshot"
    private val verKey = "surf-redis:sync:set:$id:ver"
    override val redisChannel: String = "surf-redis:sync:set:$id"

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
     * Returns a copy of the current set state.
     *
     * The returned set is a snapshot and will not reflect future updates.
     */
    fun snapshot() = lock.read { ObjectOpenHashSet(set) }

    /** @return number of elements in the set */
    fun size() = lock.read { set.size }

    /** @return `true` if [element] is present */
    fun contains(element: T) = lock.read { set.contains(element) }

    /**
     * Adds [element] to the set and replicates the change.
     *
     * @return `true` if the element was added, `false` if it was already present
     */
    fun add(element: T): Boolean {
        val added = lock.write { set.add(element) }
        if (!added) return false

        scope.launch {
            val elementJson = api.json.encodeToString(elementSerializer, element)
            publishLocalDelta(Delta.Add(elementJson))
        }

        notifyListeners(listeners, SyncSetChange.Added(element))
        return true
    }

    /**
     * Removes [element] from the set and replicates the change.
     *
     * @return `true` if the element was removed, `false` if it was not present
     */
    fun remove(element: T): Boolean {
        val removed = lock.write { set.remove(element) }
        if (!removed) return false

        scope.launch {
            val elementJson = api.json.encodeToString(elementSerializer, element)
            publishLocalDelta(Delta.Remove(elementJson))
        }

        notifyListeners(listeners, SyncSetChange.Removed(element))
        return true
    }

    /**
     * Removes all elements from the set that match the given [predicate] and replicates the changes.
     *
     * Each matching element is removed under a write lock, and each removal is replicated as a
     * separate delta to ensure consistency across nodes. Deltas are published sequentially
     * to maintain order.
     *
     * @param predicate the predicate to test each element against
     * @return `true` if any elements were removed, `false` if no elements matched the predicate
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean {
        val removedElements = mutableListOf<T>()

        lock.write {
            val iterator = set.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (predicate(element)) {
                    iterator.remove()
                    removedElements.add(element)
                }
            }
        }

        if (removedElements.isEmpty()) return false

        // Publish deltas sequentially to maintain order
        scope.launch {
            removedElements.forEach { element ->
                val elementJson = api.json.encodeToString(elementSerializer, element)
                publishLocalDelta(Delta.Remove(elementJson))
            }
        }

        removedElements.forEach { element ->
            notifyListeners(listeners, SyncSetChange.Removed(element))
        }
        return true
    }

    /**
     * Clears the set and replicates the change.
     *
     * If the set is already empty, this method is a no-op and does not publish a delta.
     */
    fun clear() {
        val hadElements = lock.write {
            val had = set.isNotEmpty()
            set.clear()
            had
        }
        if (!hadElements) return

        scope.launch {
            publishLocalDelta(Delta.Clear)
        }

        notifyListeners(listeners, SyncSetChange.Cleared)
    }

    /**
     * Registers a change listener.
     *
     * The listener is invoked for all local and remote changes.
     */
    fun addListener(listener: (SyncSetChange) -> Unit) {
        listeners += listener
    }

    /** Removes a previously registered listener. */
    fun removeListener(listener: (SyncSetChange) -> Unit) {
        listeners -= listener
    }

    override suspend fun loadSnapshot() {
        val snapshotJson = dataBucket.get().awaitSingleOrNull()
        val version = remoteVersion.get().awaitSingle()

        val loaded: Set<T> =
            if (snapshotJson.isNullOrBlank()) emptySet()
            else api.json.decodeFromString(snapshotSerializer, snapshotJson)

        lock.write {
            set.clear()
            set.addAll(loaded)
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
     * Persists the full set snapshot and version with TTL.
     *
     * Used for late joiners and recovery after missed deltas.
     */
    private suspend fun persistSnapshot(version: Long) {
        val snapshotJson = lock.read {
            api.json.encodeToString(snapshotSerializer, set)
        }

        dataBucket.set(snapshotJson, ttl.toJavaDuration()).awaitSingle()
        remoteVersion.set(version).awaitSingle()
        remoteVersion.expire(ttl.toJavaDuration()).awaitSingle()
    }

    /**
     * Periodically refreshes TTLs for snapshot and version keys.
     *
     * Ensures the set remains ephemeral and is automatically
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
     * Applies a single delta to the local set and notifies listeners.
     */
    private fun applyDelta(delta: Delta) {
        when (delta) {
            is Delta.Add -> {
                val element = api.json.decodeFromString(elementSerializer, delta.elementJson)
                val added = lock.write { set.add(element) }
                if (added) notifyListeners(listeners, SyncSetChange.Added(element))
            }

            is Delta.Remove -> {
                val element = api.json.decodeFromString(elementSerializer, delta.elementJson)
                val removed = lock.write { set.remove(element) }
                if (removed) notifyListeners(listeners, SyncSetChange.Removed(element))
            }

            Delta.Clear -> {
                val hadElements = lock.write {
                    val had = set.isNotEmpty()
                    set.clear()
                    had
                }
                if (hadElements) notifyListeners(listeners, SyncSetChange.Cleared)
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
     * Delta operations for set replication.
     */
    @Serializable
    private sealed interface Delta {
        @Serializable
        data class Add(val elementJson: String) : Delta

        @Serializable
        data class Remove(val elementJson: String) : Delta

        @Serializable
        data object Clear : Delta
    }
}
