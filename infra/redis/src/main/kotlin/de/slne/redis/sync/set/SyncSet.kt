package de.slne.redis.sync.set

import de.slne.redis.RedisApi
import de.slne.redis.sync.SyncStructure
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


class SyncSet<T : Any>(
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

    @Volatile
    private var localVersion: Long = 0L

    companion object {
        val DEFAULT_TTL = 5.minutes
    }

    override suspend fun init() {
        super.init()
        startHeartbeat()
    }

    fun snapshot() = lock.read { ObjectOpenHashSet(set) }
    fun size() = lock.read { set.size }
    fun contains(element: T) = lock.read { set.contains(element) }

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

    fun addListener(listener: (SyncSetChange) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (SyncSetChange) -> Unit) {
        listeners -= listener
    }

    override suspend fun loadSnapshot() {
        val async = api.connection.async()

        val snapshotJson = async.get(dataKey).await()
        val version = async.get(verKey).await()?.toLongOrNull() ?: 0L

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

    private suspend fun publishLocalDelta(delta: Delta) {
        val async = api.connection.async()

        val newVersion = async.incr(verKey).await()
        localVersion = newVersion

        persistSnapshot(newVersion)

        val msg = api.json.encodeToString(Envelope(newVersion, delta))
        api.pubSubConnection.async().publish(redisChannel, msg).await()
    }

    private suspend fun persistSnapshot(version: Long) {
        val snapshotJson = lock.read {
            api.json.encodeToString(snapshotSerializer, set)
        }

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
        data class Remove(val elementJson: String) : Delta

        @Serializable
        data object Clear : Delta
    }
}