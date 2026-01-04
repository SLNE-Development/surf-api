package dev.slne.surf.redis.sync.set

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.set.SyncSet.Companion.DEFAULT_TTL
import dev.slne.surf.redis.util.component1
import dev.slne.surf.redis.util.component2
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
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

        val elementJson = api.json.encodeToString(elementSerializer, element)
        publishLocalDelta(Delta.Add(elementJson)).subscribe()

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

        val elementJson = api.json.encodeToString(elementSerializer, element)
        publishLocalDelta(Delta.Remove(elementJson)).subscribe()

        notifyListeners(listeners, SyncSetChange.Removed(element))
        return true
    }

    /**
     * Removes all elements from the set that match the given [predicate] and replicates the changes.
     *
     * This operation uses atomic replication: the entire set is replaced in a single delta.
     * This ensures consistency across distributed nodes since the operation cannot interleave
     * with other deltas, avoiding race conditions and ensuring all nodes see the same final state.
     *
     * @param predicate the predicate to test each element against
     * @return `true` if any elements were removed, `false` if no elements matched the predicate
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean {
        // Perform filtering and capture the results under write lock
        val (remaining, hadRemovals, removedElements) = lock.write {
            val removed = mutableListOf<T>()
            val iterator = set.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (predicate(element)) {
                    iterator.remove()
                    removed.add(element)
                }
            }
            Triple(ObjectOpenHashSet(set), removed.isNotEmpty(), removed)
        }

        if (!hadRemovals) return false

        // Replicate using a single replace-all delta for atomic consistency
        val snapshotJson = api.json.encodeToString(snapshotSerializer, remaining)
        publishLocalDelta(Delta.ReplaceAll(snapshotJson)).subscribe()

        // Notify listeners for each removed element
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

        publishLocalDelta(Delta.Clear).subscribe()
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


    override fun loadSnapshot(): Mono<Void> {
        return dataBucket.get()
            .zipWith(remoteVersion.get())
            .flatMap<Void> { (snapshotJson, version) ->
                Mono.fromRunnable {
                    val loaded: Set<T> =
                        if (snapshotJson.isNullOrBlank()) emptySet()
                        else api.json.decodeFromString(snapshotSerializer, snapshotJson)

                    lock.write {
                        set.clear()
                        set.addAll(loaded)
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
                    Mono.empty() // duplicate / out-of-order -> ignore
                }

                else -> {
                    // gap detected -> reload snapshot (reactive)
                    loadSnapshot()
                }
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
            }
            .doOnError { t ->
                log.atSevere()
                    .withCause(t)
                    .log("Failed to publish SyncSet delta for set '$id'")
            }.then()
    }

    /**
     * Persists the full set snapshot and version with TTL.
     *
     * Used for late joiners and recovery after missed deltas.
     */
    private fun persistSnapshot(version: Long): Mono<Void> {
        return Mono.fromCallable {
            lock.read { api.json.encodeToString(snapshotSerializer, set) }
        }.flatMap { snapshotJson ->
            dataBucket.set(snapshotJson, ttl.toJavaDuration())
                .then(remoteVersion.expire(ttl.toJavaDuration()))
        }.doOnError { t ->
            log.atSevere()
                .withCause(t)
                .log("Failed to persist SyncSet snapshot for set '$id'")
        }.then()
    }

    /**
     * Periodically refreshes TTLs for snapshot and version keys.
     *
     * Ensures the set remains ephemeral and is automatically
     * removed from Redis if all nodes go offline.
     */
    private fun startHeartbeat(): Flux<Void> {
        if (ttl == Duration.ZERO || ttl.isNegative()) return Flux.empty()

        return Flux.interval((ttl / 2).toJavaDuration())
            .concatMap {
                dataBucket.expire(ttl.toJavaDuration())
                    .then(remoteVersion.expire(ttl.toJavaDuration()))
                    .onErrorResume { t ->
                        log.atSevere()
                            .withCause(t)
                            .log("Heartbeat persistSnapshot failed for '$id'")
                        Mono.empty()
                    }.then()
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

            is Delta.ReplaceAll -> {
                val snapshot = api.json.decodeFromString(snapshotSerializer, delta.snapshotJson)
                lock.write {
                    set.clear()
                    set.addAll(snapshot)
                }
                // Note: We notify Cleared for simplicity, similar to SyncList
                // Individual Removed notifications are only for the local node in removeIf()
                notifyListeners(listeners, SyncSetChange.Cleared)
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

        @Serializable
        data class ReplaceAll(val snapshotJson: String) : Delta
    }
}
