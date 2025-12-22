package de.slne.redis.sync.map

import de.slne.redis.RedisApi
import de.slne.redis.sync.SyncStructure
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


class SyncMap<K : Any, V : Any>(
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

    @Volatile
    private var localVersion: Long = 0L

    companion object {
        val DEFAULT_TTL = 5.minutes
    }

    override suspend fun init() {
        super.init()
        startHeartbeat()
    }

    fun snapshot(): Object2ObjectOpenHashMap<K, V> = lock.read { Object2ObjectOpenHashMap(map) }
    fun size(): Int = lock.read { map.size }
    fun isEmpty(): Boolean = lock.read { map.isEmpty() }
    fun get(key: K): V? = lock.read { map[key] }
    fun containsKey(key: K): Boolean = lock.read { map.containsKey(key) }


    fun addListener(listener: (SyncMapChange) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (SyncMapChange) -> Unit) {
        listeners -= listener
    }

    override suspend fun loadSnapshot() {
        val async = api.connection.async()

        val snapshotJson = async.get(dataKey).await()
        val version = async.get(verKey).await()?.toLongOrNull() ?: 0L


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

    private suspend fun publishLocalDelta(delta: Delta) {
        val async = api.connection.async()

        val newVersion = async.incr(verKey).await()
        localVersion = newVersion

        persistSnapshot(newVersion)

        val msg = api.json.encodeToString(Envelope(newVersion, delta))
        api.pubSubConnection.async().publish(redisChannel, msg).await()
    }

    private suspend fun persistSnapshot(version: Long) {
        val snapshotJson = lock.read { api.json.encodeToString(snapshotSerializer, map) }

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

    @Serializable
    private data class Envelope(
        val version: Long,
        val delta: Delta
    )

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