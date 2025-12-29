package dev.slne.surf.redis.sync.value

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.redisson.client.codec.StringCodec
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Replicated in-memory value that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds a local value of type [T].
 * - **Delta-based replication:** every update publishes a delta via Redis Pub/Sub.
 * - **Snapshot for late joiners:** the current value is stored in Redis with a TTL.
 * - **Versioning:** each update increments a global version counter.
 * - **Resync:** if a node detects a version gap, it reloads the snapshot.
 * - **Ephemeral storage:** snapshot and version keys expire automatically and are refreshed
 *   via heartbeat while nodes are alive.
 *
 * ## Defaults and initialization
 * - The value is never `null`.
 * - If no snapshot exists (e.g. first start or TTL expired), the value is initialized
 *   to [defaultValue].
 *
 * ## Consistency model
 * - Eventual consistency.
 * - Deltas are applied only if `version == localVersion + 1`.
 * - Duplicate or out-of-order deltas are ignored.
 * - Missing deltas trigger a snapshot reload.
 *
 * ## Threading / API behavior
 * - [set] is **non-suspending** and does not block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread applying the change
 *   (caller thread for local changes, Pub/Sub thread for remote changes).
 *
 * @param api owning [RedisApi] instance
 * @param id unique identifier for this value (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis operations
 * @param serializer serializer for [T]
 * @param defaultValue value used if no snapshot exists
 * @param ttl TTL for snapshot and version keys (default: [DEFAULT_TTL])
 */
class SyncValue<T : Any> internal constructor(
    api: RedisApi,
    id: String,
    scope: CoroutineScope,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    private val ttl: Duration = DEFAULT_TTL,
) : SyncStructure<T>(api, id, scope) {

    private val listeners = CopyOnWriteArrayList<(SyncValueChange) -> Unit>()

    private val dataKey = "surf-redis:sync:value:$id:snapshot"
    private val verKey = "surf-redis:sync:value:$id:ver"
    override val redisChannel: String = "surf-redis:sync:value:$id"

    private val dataBucket = api.redissonReactive.getBucket<String>(dataKey, StringCodec.INSTANCE)
    private val remoteVersion = api.redissonReactive.getAtomicLong(verKey)

    @Volatile
    private var localVersion: Long = 0L

    @Volatile
    private var value: T = defaultValue

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
     * Returns the current local value.
     *
     * This is a snapshot read and may change immediately after returning due to remote updates.
     */
    fun get(): T = lock.read { value }

    /**
     * Updates the value locally and replicates the change.
     *
     * This method returns immediately. Replication happens asynchronously on [scope].
     *
     * Listeners are notified even if the value did not change (by equality),
     * since the bus does not attempt to deduplicate logically-equal updates.
     */
    fun set(newValue: T) {
        val old = lock.write {
            val oldValue = value
            value = newValue
            oldValue
        }

        scope.launch {
            val delta = Delta.Set(api.json.encodeToString(serializer, newValue))
            publishLocalDelta(delta)
        }

        notifyListeners(listeners, SyncValueChange.Updated(newValue, old))
    }

    /**
     * Registers a change listener.
     *
     * The listener is invoked for all local and remote updates.
     */
    fun addListener(listener: (SyncValueChange) -> Unit) {
        listeners += listener
    }

    /** Removes a previously registered listener. */
    fun removeListener(listener: (SyncValueChange) -> Unit) {
        listeners -= listener
    }

    override suspend fun loadSnapshot() {
        val snapshotJson = dataBucket.get().awaitSingleOrNull()
        val version = remoteVersion.get().awaitSingle()

        val loaded =
            if (snapshotJson.isNullOrBlank()) defaultValue
            else api.json.decodeFromString(serializer, snapshotJson)

        lock.write { value = loaded }
        localVersion = version
    }

    override fun handleIncoming(message: String) {
        val envelope = api.json.decodeFromString(Envelope.serializer(), message)
        val current = localVersion

        when {
            envelope.version == current + 1 -> {
                applyDelta(envelope.delta)
                localVersion = envelope.version
            }

            envelope.version <= current -> {
                // duplicate / old -> ignore
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

        val msg = api.json.encodeToString(
            Envelope.serializer(),
            Envelope(newVersion, delta)
        )

        topic.publish(msg).awaitSingle()
    }

    /**
     * Persists the current value snapshot and version with TTL.
     *
     * Used for late joiners and recovery after missed deltas.
     */
    private suspend fun persistSnapshot(version: Long) {
        val current = lock.read { value }
        val snapshotJson = api.json.encodeToString(serializer, current)

        dataBucket.set(snapshotJson, ttl.toJavaDuration()).awaitSingle()
        remoteVersion.set(version).awaitSingle()
        remoteVersion.expire(ttl.toJavaDuration()).awaitSingle()
    }

    /**
     * Periodically refreshes TTLs for snapshot and version keys.
     *
     * Ensures the value remains ephemeral and is automatically removed from Redis
     * if all nodes go offline.
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
     * Applies a single delta to the local value and notifies listeners.
     */
    private fun applyDelta(delta: Delta) {
        when (delta) {
            is Delta.Set -> {
                val decoded = api.json.decodeFromString(serializer, delta.valueJson)
                val old = lock.write {
                    val oldValue = value
                    value = decoded
                    oldValue
                }
                notifyListeners(listeners, SyncValueChange.Updated(decoded, old))
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
     * Delta operations for value replication.
     */
    @Serializable
    private sealed interface Delta {
        @Serializable
        data class Set(val valueJson: String) : Delta
    }
}