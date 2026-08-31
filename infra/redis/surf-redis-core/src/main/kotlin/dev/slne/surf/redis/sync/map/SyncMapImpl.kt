package dev.slne.surf.redis.sync.map

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractStreamSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure.SimpleVersionedSnapshot
import dev.slne.surf.redis.sync.SyncValueCodec
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.redis.util.RedisExpirableUtils
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration

class SyncMapImpl<K : Any, V : Any> internal constructor(
    api: RedisApi,
    id: String,
    ttl: Duration,
    private val keyCodec: SyncValueCodec<K>,
    private val valueCodec: SyncValueCodec<V>,
) : AbstractStreamSyncStructure<SyncMapChange<K, V>, SimpleVersionedSnapshot<Map<String, String>>>(
    api,
    id,
    ttl,
    Registry,
    NAMESPACE,
    CodecDescriptor.of(keyCodec, valueCodec)
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
        private const val SNAPSHOT_SCRIPT = "snapshot"
        private const val REPLACE_IF_EQUALS_SCRIPT = "replace-if-equals"
        private const val REMOVE_IF_EQUALS_SCRIPT = "remove-if-equals"

        private object Registry : LuaScriptRegistry("lua/sync/map") {
            init {
                load(PUT_SCRIPT)
                load(REMOVE_SCRIPT)
                load(REMOVE_MANY_SCRIPT)
                load(CLEAR_SCRIPT)

                load(SNAPSHOT_SCRIPT)
                load(REPLACE_IF_EQUALS_SCRIPT)
                load(REMOVE_IF_EQUALS_SCRIPT)
            }
        }
    }

    private val map = Object2ObjectOpenHashMap<K, V>()
    private val remoteMap by lazy {
        api.redissonReactive.getMap<String, String>(
            dataKey,
            StringCodec.INSTANCE
        )
    }

    override fun init(): Mono<Void> {
        return super.init()
            .doOnSuccess {
                trackDisposable(RedisExpirableUtils.refreshContinuously(ttl, remoteMap))
            }
            .then()
    }

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
        val previous = lock.write {
            map.put(key, value)
        }

        notifyListeners(SyncMapChange.Put(key, value, previous))
        putRemote(key, value)

        return previous
    }

    override fun remove(key: K): V? {
        val old = lock.write { map.remove(key) } ?: return null

        removeRemote(key)
        notifyListeners(SyncMapChange.Removed(key, old))

        return old
    }

    override fun removeIf(predicate: (K, V) -> Boolean): Boolean {
        val keysToRemove = ObjectArrayList<K>()
        val removedLocal = ObjectArrayList<Pair<K, V>>()
        lock.write {
            val it = map.object2ObjectEntrySet().fastIterator()
            while (it.hasNext()) {
                val e = it.next()
                if (predicate(e.key, e.value)) {
                    keysToRemove.add(e.key)
                    removedLocal.add(e.key to e.value)
                    it.remove()
                }
            }
        }
        if (keysToRemove.isEmpty) return false

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

    override fun loadFromRemote0(): Mono<SimpleVersionedSnapshot<Map<String, String>>> {
        return readAtomicSnapshot(SNAPSHOT_SCRIPT)
            .map { raw ->
                require(raw.isNotEmpty()) {
                    "Empty snapshot result for SyncMap '$id'"
                }

                val payloadSize = raw.size - 1

                require(payloadSize % 2 == 0) {
                    "Malformed map snapshot for '$id': expected key/value pairs"
                }

                val version = raw.last().toString().toLong()
                val values = Object2ObjectOpenHashMap<String, String>(payloadSize / 2)

                var index = 0
                while (index < payloadSize) {
                    values[raw[index].toString()] = raw[index + 1].toString()
                    index += 2
                }

                SimpleVersionedSnapshot(values, version)
            }
    }

    override suspend fun getRemote(key: K): V? {
        return remoteMap.get(encodeKey(key))
            .awaitSingleOrNull()
            ?.let(::decodeValue)
    }

    override suspend fun putAndAwait(key: K, value: V): V? {
        val previous = lock.write {
            map.put(key, value)
        }

        notifyListeners(SyncMapChange.Put(key, value, previous))

        putRemoteAwait(key, value).awaitSingle()

        return previous
    }

    private fun putRemoteAwait(key: K, value: V): Mono<Long> {
        return writeToRemoteAwait(
            PUT_SCRIPT,
            EVENT_PUT,
            encodeKey(key),
            encodeValue(value),
        )
    }

    override suspend fun removeAndAwait(key: K): V? {
        val old = lock.write {
            map.remove(key)
        } ?: return null

        notifyListeners(SyncMapChange.Removed(key, old))

        writeToRemoteAwait(
            REMOVE_SCRIPT,
            EVENT_REMOVE,
            encodeKey(key),
        ).awaitSingle()

        return old
    }

    override suspend fun removeIfAndAwait(
        predicate: (K, V) -> Boolean,
    ): Boolean {
        val keysToRemove = ObjectArrayList<K>()
        val removedLocal = ObjectArrayList<Pair<K, V>>()

        lock.write {
            val iterator = map.object2ObjectEntrySet().fastIterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()

                if (predicate(entry.key, entry.value)) {
                    keysToRemove.add(entry.key)
                    removedLocal.add(entry.key to entry.value)
                    iterator.remove()
                }
            }
        }

        if (keysToRemove.isEmpty) {
            return false
        }

        for ((key, value) in removedLocal) {
            notifyListeners(SyncMapChange.Removed(key, value))
        }

        val encodedKeys = Array(keysToRemove.size) { index ->
            encodeKey(keysToRemove[index])
        }

        writeBatchToRemoteAwait(
            REMOVE_MANY_SCRIPT,
            EVENT_REMOVE,
            *encodedKeys,
        ).awaitSingle()

        return true
    }

    override suspend fun clearAndAwait() {
        val hadEntries = lock.write {
            val had = map.isNotEmpty()
            map.clear()
            had
        }

        if (!hadEntries) {
            return
        }

        notifyListeners(SyncMapChange.Cleared())

        writeToRemoteAwait(
            CLEAR_SCRIPT,
            EVENT_CLEAR,
        ).awaitSingle()
    }

    override suspend fun replaceIfEqualsAndAwait(
        key: K,
        expectedValue: V,
        newValue: V,
    ): Boolean {
        val version = writeToRemoteAwait(
            REPLACE_IF_EQUALS_SCRIPT,
            EVENT_PUT,
            encodeKey(key),
            encodeValue(expectedValue),
            encodeValue(newValue),
        ).awaitSingle()

        if (version == 0L) {
            return false
        }

        var notify = false
        var resync = false

        lock.write {
            when (map[key]) {
                expectedValue -> {
                    map[key] = newValue
                    notify = true
                }

                newValue -> Unit

                else -> {
                    resync = true
                }
            }
        }

        if (notify) {
            notifyListeners(
                SyncMapChange.Put(
                    key,
                    newValue,
                    expectedValue,
                )
            )
        }

        if (resync) {
            requestResync()
        }

        return true
    }

    override suspend fun removeIfEqualsAndAwait(
        key: K,
        expectedValue: V,
    ): Boolean {
        val version = writeToRemoteAwait(
            REMOVE_IF_EQUALS_SCRIPT,
            EVENT_REMOVE,
            encodeKey(key),
            encodeValue(expectedValue),
        ).awaitSingle()

        if (version == 0L) {
            return false
        }

        var notify = false
        var resync = false

        lock.write {
            when (map[key]) {
                expectedValue -> {
                    map.remove(key)
                    notify = true
                }

                null -> Unit

                else -> {
                    resync = true
                }
            }
        }

        if (notify) {
            notifyListeners(
                SyncMapChange.Removed(
                    key,
                    expectedValue,
                )
            )
        }

        if (resync) {
            requestResync()
        }

        return true
    }

    override fun overrideFromRemote(raw: SimpleVersionedSnapshot<Map<String, String>>) {
        val rawValue = raw.value
        val decoded = Object2ObjectOpenHashMap<K, V>(rawValue.size)
        for ((k, v) in rawValue) {
            decoded[decodeKey(k)] = decodeValue(v)
        }

        lock.write {
            map.clear()
            map.putAll(decoded)
        }

        super.overrideFromRemote(raw)
    }

    private fun putRemote(key: K, value: V) {
        writeToRemote(PUT_SCRIPT, EVENT_PUT, encodeKey(key), encodeValue(value))
    }

    private fun removeRemote(key: K) {
        writeToRemote(REMOVE_SCRIPT, EVENT_REMOVE, encodeKey(key))
    }

    private fun removeManyRemote(keys: List<K>) {
        val encKeys = Array(keys.size) { i -> encodeKey(keys[i]) }
        writeBatchToRemote(REMOVE_MANY_SCRIPT, EVENT_REMOVE, *encKeys)
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
        val encodedKey = data.payload(0)
        val encodedVal = data.payload(1)
        val encodedOldVal = data.payloadOrNull(2)

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
        val encodedKey = data.payload(0)
        val encodedOldVal = data.payload(1)

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

    private fun encodeKey(key: K): String = keyCodec.encode(key)
    private fun decodeKey(raw: String): K = keyCodec.decode(raw)
    private fun encodeValue(value: V): String = valueCodec.encode(value)
    private fun decodeValue(raw: String): V = valueCodec.decode(raw)

    private object CodecDescriptor {
        fun of(
            keyCodec: SyncValueCodec<*>,
            valueCodec: SyncValueCodec<*>
        ): String? {
            val key = keyCodec.descriptor
            val value = valueCodec.descriptor
            if (key == null && value == null) return null
            return "map:${key ?: "json"}:${value ?: "json"}"
        }
    }
}
