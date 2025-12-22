package de.slne.redis.sync.value

import de.slne.redis.RedisApi
import de.slne.redis.sync.SyncStructure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


class SyncValue<T : Any>(
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

    @Volatile
    private var localVersion: Long = 0L

    @Volatile
    private var value: T = defaultValue

    companion object {
        val DEFAULT_TTL = 5.minutes
    }

    override suspend fun init() {
        super.init()
        startHeartbeat()
    }

    fun get(): T = lock.read { value }

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

    fun addListener(listener: (SyncValueChange) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (SyncValueChange) -> Unit) {
        listeners -= listener
    }

    override suspend fun loadSnapshot() {
        val async = api.connection.async()

        val snapshotJson = async.get(dataKey).await()
        val version = async.get(verKey).await()?.toLongOrNull() ?: 0L

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

    private suspend fun publishLocalDelta(delta: Delta) {
        val async = api.connection.async()

        val newVersion = async.incr(verKey).await()
        localVersion = newVersion

        persistSnapshot(newVersion)

        val msg = api.json.encodeToString(
            Envelope.serializer(),
            Envelope(newVersion, delta)
        )

        api.pubSubConnection.async()
            .publish(redisChannel, msg)
            .await()
    }

    private suspend fun persistSnapshot(version: Long) {
        val current = lock.read { value }
        val snapshotJson = api.json.encodeToString(serializer, current)

        val async = api.connection.async()
        async.setex(dataKey, ttl.inWholeSeconds, snapshotJson).await()
        async.setex(verKey, ttl.inWholeSeconds, version.toString()).await()
    }

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

    @Serializable
    private data class Envelope(
        val version: Long,
        val delta: Delta
    )

    @Serializable
    private sealed interface Delta {
        @Serializable
        data class Set(val valueJson: String) : Delta
    }
}