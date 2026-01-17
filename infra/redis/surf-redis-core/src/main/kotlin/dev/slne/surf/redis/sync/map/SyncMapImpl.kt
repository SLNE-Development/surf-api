package dev.slne.surf.redis.sync.map

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractStreamSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure.SimpleVersionedSnapshot
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.serialization.KSerializer
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class SyncMapImpl<K : Any, V : Any>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
) : AbstractStreamSyncStructure<SyncMapChange<K, V>, SimpleVersionedSnapshot<Map<String, String>>>(
    api,
    id,
    ttl,
    Registry
), SyncMap<K, V> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "map:"

        private const val EVENT_PUT = "P"
        private const val EVENT_REMOVE = "R"
        private const val EVENT_CLEAR = "C"

        private const val PUT_SCRIPT = "put"
        private const val REMOVE_SCRIPT = "remove"
        private const val REMOVE_MANY_SCRIPT = "remove-many"
        private const val CLEAR_SCRIPT = "clear"

        private object Registry : LuaScriptRegistry("lua/sync/map") {
            init {
                load(PUT_SCRIPT)
                load(REMOVE_SCRIPT)
                load(REMOVE_MANY_SCRIPT)
                load(CLEAR_SCRIPT)
            }
        }
    }

    override val structureNamespace = NAMESPACE


    private val map = Object2ObjectOpenHashMap<K, V>()
    private val remoteMap by lazy { api.redissonReactive.getMap<String, String>(dataKey, StringCodec.INSTANCE) }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        remoteMap.addListener(DeletedObjectListener { requestResync() }),
        remoteMap.addListener(ExpiredObjectListener { requestResync() })
    )

    override fun unregisterListener(id: Int): Mono<*> = remoteMap.removeListener(id)

    override fun snapshot() = lock.read { Object2ObjectOpenHashMap(map) }
    override fun size() = lock.read { map.size }
    override fun containsKey(key: K) = lock.read { map.containsKey(key) }
    override fun get(key: K): V? = lock.read { map[key] }
    override fun isEmpty() = lock.read { map.isEmpty() }

    override fun put(key: K, value: V): V? {
        val previous = lock.write { map.put(key, value) }

        putRemote(key, value)
        notifyListeners(SyncMapChange.Put(key, value, previous))

        return previous
    }

    override fun remove(key: K): V? {
        val old = lock.write { map.remove(key) } ?: return null

        removeRemote(key)
        notifyListeners(SyncMapChange.Removed(key, old))

        return old
    }

    override fun removeIf(predicate: (K, V) -> Boolean): Boolean {
        val keysToRemove = lock.write {
            val keys = ObjectArrayList<K>()
            val it = map.object2ObjectEntrySet().fastIterator()
            while (it.hasNext()) {
                val e = it.next()
                if (predicate(e.key, e.value)) keys.add(e.key)
            }
            keys
        }
        if (keysToRemove.isEmpty) return false

        val removedLocal = ObjectArrayList<Pair<K, V>>(keysToRemove.size)
        lock.write {
            for (k in keysToRemove) {
                val old = map.remove(k) ?: continue
                removedLocal.add(k to old)
            }
        }

        removeManyRemote(keysToRemove)
        removedLocal.forEach { (k, v) -> notifyListeners(SyncMapChange.Removed(k, v)) }

        return true
    }

    override fun clear() {
        val had = lock.write {
            val h = map.isNotEmpty()
            map.clear()
            h
        }
        if (!had) return

        clearRemote()
        notifyListeners(SyncMapChange.Cleared())
    }

    override fun loadFromRemote0(): Mono<SimpleVersionedSnapshot<Map<String, String>>> = Mono.zip(
        remoteMap.readAllMap(),
        versionCounter.get().onErrorReturn(0L)
    ).map { SimpleVersionedSnapshot.fromTuple(it) }

    override fun overrideFromRemote(raw: SimpleVersionedSnapshot<Map<String, String>>) {
        val decoded = raw.value.map { (k, v) -> decodeKey(k) to decodeValue(v) }.toMap()
        lock.write {
            map.clear()
            map.putAll(decoded)
        }

        super.overrideFromRemote(raw)
    }

    override fun refreshTtl0(): Mono<*> = Mono.`when`(
        remoteMap.expire(ttl.toJavaDuration())
    )

    private fun putRemote(key: K, value: V) {
        writeToRemote(PUT_SCRIPT, EVENT_PUT, encodeKey(key), encodeValue(value))
    }

    private fun removeRemote(key: K) {
        writeToRemote(REMOVE_SCRIPT, EVENT_REMOVE, encodeKey(key))
    }

    private fun removeManyRemote(keys: List<K>) {
        val encKeys = keys.map(::encodeKey).toTypedArray()
        writeToRemote(REMOVE_MANY_SCRIPT, EVENT_REMOVE, *encKeys)
    }

    private fun clearRemote() {
        writeToRemote(CLEAR_SCRIPT, EVENT_CLEAR)
    }

    override fun onStreamEvent(type: String, data: StreamEventData) = when (type) {
        EVENT_PUT -> onPutEvent(data)
        EVENT_REMOVE -> onRemoveEvent(data)
        EVENT_CLEAR -> onCleared(data)
        else -> log.atWarning().log("Unknown message type '$type' received from SyncMap '$id'")
    }

    private fun onPutEvent(data: StreamEventData) {
        val encodedKey = data.payload[0]
        val encodedVal = data.payload[1]
        val encodedOldVal = data.payload.getOrNull(2)

        val decodedKey = decodeKey(encodedKey)
        val decodedVal = decodeValue(encodedVal)
        val decodedOldVal = encodedOldVal?.let { decodeValue(it) }

        val ok = lock.write {
            val cur = map[decodedKey]

            // Updates map entry if preconditions are satisfied
            if (decodedOldVal == null) {
                if (cur != null) return@write false
                map[decodedKey] = decodedVal
                true
            } else {
                if (cur == null) return@write false
                if (cur != decodedOldVal) return@write false
                map[decodedKey] = decodedVal
                true
            }
        }

        if (!ok) return requestResync()
        notifyListeners(SyncMapChange.Put(decodedKey, decodedVal, decodedOldVal))
    }

    private fun onRemoveEvent(data: StreamEventData) {
        val encodedKey = data.payload[0]
        val encodedOldVal = data.payload[1]

        val decodedKey = decodeKey(encodedKey)
        val decodedOldVal = decodeValue(encodedOldVal)

        val ok = lock.write {
            val cur = map[decodedKey] ?: return@write false
            if (cur != decodedOldVal) return@write false
            map.remove(decodedKey)
            true
        }
        if (!ok) return requestResync()
        notifyListeners(SyncMapChange.Removed(decodedKey, decodedOldVal))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCleared(data: StreamEventData) {
        val had = lock.write {
            val h = map.isNotEmpty()
            map.clear()
            h
        }

        if (had) notifyListeners(SyncMapChange.Cleared())
    }

    private fun encodeKey(key: K): String = api.json.encodeToString(keySerializer, key)
    private fun decodeKey(raw: String): K = api.json.decodeFromString(keySerializer, raw)
    private fun encodeValue(value: V): String = api.json.encodeToString(valueSerializer, value)
    private fun decodeValue(raw: String): V = api.json.decodeFromString(valueSerializer, raw)
}