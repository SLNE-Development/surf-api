package dev.slne.surf.redis.sync.list

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.util.component1
import dev.slne.surf.redis.util.component2
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
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
 * Replicated in-memory list that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds its own local list instance.
 * - **Deltas via Pub/Sub:** mutations publish a delta to [redisChannel].
 * - **Snapshot for late joiners:** the full list is stored in Redis under [dataKey] with a TTL.
 * - **Versioning:** each mutation increments [verKey]. Deltas carry a monotonically increasing version.
 * - **Resync:** if a node detects a version gap, it reloads the snapshot.
 * - **Ephemeral storage:** snapshot + version keys expire automatically. A heartbeat refreshes the TTL while
 *   at least one node is alive.
 *
 * ## Consistency / ordering
 * - Eventual consistency across nodes.
 * - Deltas are applied only if `version == localVersion + 1`.
 * - Duplicate/out-of-order deltas (`version <= localVersion`) are ignored.
 * - Missing deltas trigger a snapshot reload.
 *
 * ## Threading / API behavior
 * - Public mutating methods (e.g. [add], [set]) are **non-suspending** and do not block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread that applies the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 *
 * @param api owning [RedisApi] (must already be connected and have Pub/Sub available)
 * @param id unique identifier for this list (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis I/O and background tasks (heartbeat/resync)
 * @param elementSerializer serializer for list elements
 * @param ttl TTL for snapshot/version keys; refreshed periodically while the list is active
 */
class SyncList<T : Any> internal constructor(
    api: RedisApi,
    id: String,
    scope: CoroutineScope,
    private val elementSerializer: KSerializer<T>,
    private val ttl: Duration = DEFAULT_TTL,
) : SyncStructure<T>(api, id, scope) {

    private val list = ObjectArrayList<T>()
    private val snapshotSerializer = ListSerializer(elementSerializer)

    private val listeners = CopyOnWriteArrayList<(SyncListChange) -> Unit>()

    private val dataKey = "surf-redis:sync:list:$id:snapshot"
    private val verKey = "surf-redis:sync:list:$id:ver"
    override val redisChannel: String = "surf-redis:sync:list:$id"

    private val dataBucket by lazy { api.redissonReactive.getBucket<String>(dataKey, StringCodec.INSTANCE) }
    private val remoteVersion by lazy { api.redissonReactive.getAtomicLong(verKey) }

    @Volatile
    private var localVersion: Long = 0L

    companion object {
        private val log = logger()

        /**
         * Default TTL for the snapshot and version keys.
         *
         * The heartbeat refreshes the TTL periodically to keep the list ephemeral.
         */
        val DEFAULT_TTL = 5.minutes
    }

    override fun setupSubscription(): Flux<Void> {
        return super.setupSubscription()
            .mergeWith(startHeartbeat())
    }

    /**
     * Returns a copy of the current list state.
     *
     * The returned list is a snapshot and will not reflect later updates.
     */
    fun snapshot() = lock.read { ObjectArrayList(list) }

    /** @return current number of elements in the list */
    fun size() = lock.read { list.size }

    /** @return element at [index] */
    fun get(index: Int): T = lock.read { list[index] }

    /**
     * Appends [element] to the end of the list.
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     */
    fun add(element: T) {
        val index = lock.write {
            list.add(element)
            list.lastIndex
        }

        val elementJson = api.json.encodeToString(elementSerializer, element)
        publishLocalDelta(Delta.Add(elementJson)).subscribe()

        notifyListeners(listeners, SyncListChange.Added(index, element))
    }

    /**
     * Inserts [element] at [index].
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     */
    fun add(index: Int, element: T) {
        lock.write { list.add(index, element) }

        val elementJson = api.json.encodeToString(elementSerializer, element)
        publishLocalDelta(Delta.AddAt(index, elementJson)).subscribe()

        notifyListeners(listeners, SyncListChange.Added(index, element))
    }

    /**
     * Replaces the element at [index] with [element].
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the previous value at [index]
     */
    fun set(index: Int, element: T): T {
        val oldValue = lock.write { list.set(index, element) }

        val elementJson = api.json.encodeToString(elementSerializer, element)
        publishLocalDelta(Delta.Set(index, elementJson)).subscribe()

        notifyListeners(listeners, SyncListChange.Updated(index, element, oldValue))

        return oldValue
    }

    /**
     * Removes the element at [index].
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the removed element
     */
    fun removeAt(index: Int): T {
        val removed = lock.write { list.removeAt(index) }

        publishLocalDelta(Delta.RemoveAt(index)).subscribe()

        notifyListeners(listeners, SyncListChange.Removed(index, removed))

        return removed
    }

    /**
     * Removes all elements from the list that match the given [predicate] and replicates the changes.
     *
     * This operation uses atomic replication: the entire list is replaced in a single delta.
     * This ensures consistency across distributed nodes since the operation cannot interleave
     * with other deltas in the middle of a Clear/Add sequence, avoiding ordering divergence.
     *
     * @param predicate the predicate to test each element against
     * @return `true` if any elements were removed, `false` if no elements matched the predicate
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean {
        // Perform filtering and capture the results under write lock
        val (remaining, hadRemovals) = lock.write {
            val filtered = list.filterNot(predicate)
            val removals = list.size != filtered.size
            list.clear()
            list.addAll(filtered)
            filtered to removals
        }

        if (!hadRemovals) return false

        // Replicate using a single replace-all delta for atomic consistency
        val snapshotJson = api.json.encodeToString(snapshotSerializer, remaining)
        publishLocalDelta(Delta.ReplaceAll(snapshotJson)).subscribe()

        // Notify listeners - single Cleared event for simplicity
        notifyListeners(listeners, SyncListChange.Cleared)

        return true
    }

    /**
     * Clears the list.
     *
     * The local list is cleared immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     */
    fun clear() {
        lock.write { list.clear() }

        publishLocalDelta(Delta.Clear).subscribe()

        notifyListeners(listeners, SyncListChange.Cleared)
    }

    /**
     * Registers a change listener.
     *
     * The listener is invoked for all local and remote changes.
     */
    fun addListener(listener: (SyncListChange) -> Unit) {
        listeners += listener
    }

    /** Removes a previously registered listener. */
    fun removeListener(listener: (SyncListChange) -> Unit) {
        listeners -= listener
    }

    override fun loadSnapshot(): Mono<Void> {
        return dataBucket.get()
            .zipWith(remoteVersion.get())
            .flatMap { (snapshotJson, version) ->
                Mono.fromCallable {
                    val loaded = if (snapshotJson.isNullOrBlank()) emptyList()
                    else api.json.decodeFromString(snapshotSerializer, snapshotJson)

                    lock.write {
                        list.clear()
                        list.addAll(loaded)
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
     * - increments [verKey],
     * - persists snapshot + version with TTL,
     * - publishes the delta envelope via Pub/Sub.
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
                    .log("Failed to publish local delta for SyncList '$id'")
            }.then()
    }

    /**
     * Persists the full list snapshot and current [version] with TTL.
     *
     * This is used both for late joiners and as a recovery mechanism when deltas are missed.
     */
    private fun persistSnapshot(version: Long): Mono<Void> {
        return Mono.fromCallable {
            lock.read { api.json.encodeToString(snapshotSerializer, list) }
        }.flatMap { snapshotJson ->
            dataBucket.set(snapshotJson, ttl.toJavaDuration())
                .then(remoteVersion.expire(ttl.toJavaDuration()))
        }.doOnError { t ->
            log.atSevere()
                .withCause(t)
                .log("Failed to persist snapshot for SyncList '$id'")
        }.then()
    }

    /**
     * Periodically refreshes the TTL of the snapshot/version keys.
     *
     * This keeps the structure ephemeral:
     * - if no node is alive, TTL expires and Redis state disappears automatically.
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
                            .log("Failed to refresh heartbeat for SyncList '$id'")
                        Mono.empty()
                    }.then()
            }
    }

    /**
     * Applies a delta to the local list and notifies listeners.
     *
     * Index validation is performed for operations that rely on indices.
     */
    private fun applyDelta(delta: Delta) {
        when (delta) {
            is Delta.Add -> {
                val element = api.json.decodeFromString(elementSerializer, delta.elementJson)
                val index = lock.write {
                    list.add(element)
                    list.lastIndex
                }
                notifyListeners(listeners, SyncListChange.Added(index, element))
            }

            is Delta.AddAt -> {
                val element = api.json.decodeFromString(elementSerializer, delta.elementJson)
                val index = delta.index
                lock.write {
                    if (index < 0 || index > list.size) return
                    list.add(index, element)
                }
                notifyListeners(listeners, SyncListChange.Added(index, element))
            }

            is Delta.Set -> {
                val element = api.json.decodeFromString(elementSerializer, delta.elementJson)
                val index = delta.index
                val old = lock.write {
                    list.set(index, element)
                }
                notifyListeners(listeners, SyncListChange.Updated(index, element, old))
            }

            is Delta.RemoveAt -> {
                val index = delta.index
                val removed = lock.write {
                    if (index < 0 || index >= list.size) return
                    list.removeAt(index)
                }
                notifyListeners(listeners, SyncListChange.Removed(index, removed))
            }

            Delta.Clear -> {
                lock.write { list.clear() }
                notifyListeners(listeners, SyncListChange.Cleared)
            }

            is Delta.ReplaceAll -> {
                val snapshot = api.json.decodeFromString(snapshotSerializer, delta.snapshotJson)
                lock.write {
                    list.clear()
                    list.addAll(snapshot)
                }
                notifyListeners(listeners, SyncListChange.Cleared)
            }
        }
    }

    @Serializable
    private data class Envelope(
        val version: Long,
        val delta: Delta
    )

    @Serializable
    private sealed interface Delta {
        @Serializable
        data class Add(val elementJson: String) : Delta

        @Serializable
        data class AddAt(val index: Int, val elementJson: String) : Delta

        @Serializable
        data class Set(val index: Int, val elementJson: String) : Delta

        @Serializable
        data class RemoveAt(val index: Int) : Delta

        @Serializable
        data object Clear : Delta

        @Serializable
        data class ReplaceAll(val snapshotJson: String) : Delta
    }
}
