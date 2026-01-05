package dev.slne.surf.redis.sync.map

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.serialization.KSerializer
import org.intellij.lang.annotations.Language
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.api.RScript
import org.redisson.client.codec.StringCodec
import org.redisson.client.protocol.RedisCommands
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
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
) : AbstractSyncStructure<SyncMapChange<K, V>, SyncMapImpl.Snapshot>(api, id, ttl),
    SyncMap<K, V> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "map:"
        private const val DELIM = "\u0000"

        private val instanceId = UUID.randomUUID().toString().replace("-", "")

        @Language("Lua")
        private val LUA_PUT = """
            local old = redis.call('HGET', KEYS[1], ARGV[4])
            if old ~= false and old == ARGV[5] then
                return 0
            end

            redis.call('HSET', KEYS[1], ARGV[4], ARGV[5])
            local ver = redis.call('INCR', KEYS[4])

            if old == false then
                local msg = tostring(ver) .. ARGV[3] .. ARGV[1] .. ARGV[3] .. ARGV[4] .. ARGV[3] .. ARGV[5]
                redis.call(ARGV[2], KEYS[2], msg)
            else
                local msg = tostring(ver) .. ARGV[3] .. ARGV[1] .. ARGV[3] .. ARGV[4] .. ARGV[3] .. ARGV[5] .. ARGV[3] .. old
                redis.call(ARGV[2], KEYS[3], msg)
            end

            return ver
        """.trimIndent()

        @Language("Lua")
        private val LUA_REMOVE = """
            local old = redis.call('HGET', KEYS[1], ARGV[4])
            if old == false then
                return 0
            end

            redis.call('HDEL', KEYS[1], ARGV[4])
            local ver = redis.call('INCR', KEYS[3])

            local msg = tostring(ver) .. ARGV[3] .. ARGV[1] .. ARGV[3] .. ARGV[4] .. ARGV[3] .. old
            redis.call(ARGV[2], KEYS[2], msg)

            return ver
        """.trimIndent()

        @Language("Lua")
        private val LUA_REMOVE_MANY = """
            local removedCount = 0
            for i = 4, #ARGV do
                local k = ARGV[i]
                local old = redis.call('HGET', KEYS[1], k)
                if old ~= false then
                    redis.call('HDEL', KEYS[1], k)
                    removedCount = removedCount + 1

                    local ver = redis.call('INCR', KEYS[3])
                    local msg = tostring(ver) .. ARGV[3] .. ARGV[1] .. ARGV[3] .. k .. ARGV[3] .. old
                    redis.call(ARGV[2], KEYS[2], msg)
                end
            end
            return removedCount
        """.trimIndent()

        @Language("Lua")
        private val LUA_CLEAR = """
            local existed = redis.call('EXISTS', KEYS[1])
            if existed == 0 then
                return 0
            end

            redis.call('DEL', KEYS[1])
            local ver = redis.call('INCR', KEYS[3])

            local msg = tostring(ver) .. ARGV[3] .. ARGV[1]
            redis.call(ARGV[2], KEYS[2], msg)

            return ver
        """.trimIndent()
    }

    private val namespace = "$NAMESPACE$id:"
    private val dataKey = "${namespace}snapshot"
    private val versionKey = "${namespace}version"
    private val createdChannel = "${namespace}created"
    private val updatedChannel = "${namespace}updated"
    private val removedChannel = "${namespace}removed"
    private val clearedChannel = "${namespace}cleared"

    private val map = Object2ObjectOpenHashMap<K, V>()

    private val remoteMap by lazy { api.redissonReactive.getMap<String, String>(dataKey, StringCodec.INSTANCE) }
    private val versionCounter by lazy { api.redissonReactive.getAtomicLong(versionKey) }
    private val script by lazy { api.redissonReactive.getScript(StringCodec.INSTANCE) }

    private val createdTopic by lazy { api.redissonReactive.getTopic(createdChannel, StringCodec.INSTANCE) }
    private val updatedTopic by lazy { api.redissonReactive.getTopic(updatedChannel, StringCodec.INSTANCE) }
    private val removedTopic by lazy { api.redissonReactive.getTopic(removedChannel, StringCodec.INSTANCE) }
    private val clearedTopic by lazy { api.redissonReactive.getTopic(clearedChannel, StringCodec.INSTANCE) }

    private val topicListenerIds = ConcurrentHashMap<String, MutableCollection<Int>>()

    private val lastVersion = AtomicLong(0L)
    private val bootstrapped = AtomicBoolean(false)

    override fun init(): Mono<Void> {
        return super.init()
            .then(createdTopic.addListener(String::class.java) { _, msg -> onCreated(msg) }
                .doOnNext { addTopicListenerId(createdChannel, it) })
            .then(updatedTopic.addListener(String::class.java) { _, msg -> onUpdated(msg) }
                .doOnNext { addTopicListenerId(updatedChannel, it) })
            .then(removedTopic.addListener(String::class.java) { _, msg -> onRemoved(msg) }
                .doOnNext { addTopicListenerId(removedChannel, it) })
            .then(clearedTopic.addListener(String::class.java) { _, msg -> onCleared(msg) }
                .doOnNext { addTopicListenerId(clearedChannel, it) })
            .then()
    }

    override fun dispose() {
        topicListenerIds[createdChannel]?.forEach { createdTopic.removeListener(it).subscribe() }
        topicListenerIds[updatedChannel]?.forEach { updatedTopic.removeListener(it).subscribe() }
        topicListenerIds[removedChannel]?.forEach { removedTopic.removeListener(it).subscribe() }
        topicListenerIds[clearedChannel]?.forEach { clearedTopic.removeListener(it).subscribe() }
        topicListenerIds.clear()
        super.dispose()
    }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        remoteMap.addListener(DeletedObjectListener { onKeyGone() }),
        remoteMap.addListener(ExpiredObjectListener { onKeyGone() })
    )

    override fun unregisterListener(id: Int): Mono<*> = remoteMap.removeListener(id)

    private fun addTopicListenerId(name: String, id: Int) {
        val ids = topicListenerIds.computeIfAbsent(name) { ConcurrentHashMap.newKeySet() }
        ids.add(id)
    }

    override fun snapshot() = lock.read { Object2ObjectOpenHashMap(map) }
    override fun size() = lock.read { map.size }
    override fun containsKey(key: K) = lock.read { map.containsKey(key) }
    override fun get(key: K): V? = lock.read { map[key] }
    override fun isEmpty() = lock.read { map.isEmpty() }

    override fun put(key: K, value: V): V? {
        val (old, changed) = lock.write {
            val previous = map.put(key, value)
            val changed = previous != value
            previous to changed
        }
        if (!changed) return old

        val encKey = encodeKey(key)
        val encVal = encodeValue(value)

        notifyListeners(SyncMapChange.Put(key, value, old))

        putRemote(encKey, encVal).subscribe(
            { ver -> if (ver > 0) lastVersion.accumulateAndGet(ver) { a, b -> maxOf(a, b) } },
            { e ->
                log.atSevere().withCause(e).log("Failed to put key in SyncMap '$id'")
                loadFromRemote().subscribe()
            }
        )

        return old
    }

    override fun remove(key: K): V? {
        val old = lock.write { map.remove(key) } ?: return null

        notifyListeners(SyncMapChange.Removed(key, old))

        val encKey = encodeKey(key)
        removeRemote(encKey).subscribe(
            { ver -> if (ver > 0) lastVersion.accumulateAndGet(ver) { a, b -> maxOf(a, b) } },
            { e ->
                log.atSevere().withCause(e).log("Failed to remove key in SyncMap '$id'")
                loadFromRemote().subscribe()
            }
        )
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
        if (removedLocal.isEmpty) return false

        removedLocal.forEach { (k, v) -> notifyListeners(SyncMapChange.Removed(k, v)) }

        val encKeys = removedLocal.map { (k, _) -> encodeKey(k) }
        removeManyRemote(encKeys).subscribe(
            { /* Success */ },
            { e ->
                log.atSevere().withCause(e).log("Failed to removeIf keys in SyncMap '$id'")
                loadFromRemote().subscribe()
            }
        )
        return true
    }

    override fun clear() {
        val had = lock.write {
            val h = map.isNotEmpty()
            map.clear()
            h
        }
        if (!had) return

        notifyListeners(SyncMapChange.Cleared())

        clearRemote().subscribe(
            { ver -> if (ver > 0) lastVersion.accumulateAndGet(ver) { a, b -> maxOf(a, b) } },
            { e ->
                log.atSevere().withCause(e).log("Failed to clear SyncMap '$id'")
                loadFromRemote().subscribe()
            }
        )
    }

    override fun loadFromRemote0(): Mono<Snapshot> =
        Mono.zip(
            remoteMap.readAllMap(),
            versionCounter.get().onErrorReturn(0L)
        ).map(Snapshot::fromTuple)

    override fun overrideFromRemote(raw: Snapshot) {
        lock.write {
            map.clear()
            raw.entries.forEach { (encK, encV) ->
                try {
                    val k = decodeKey(encK)
                    val v = decodeValue(encV)
                    map[k] = v
                } catch (e: Throwable) {
                    log.atWarning().withCause(e).log("Failed to decode entry in SyncMap '$id'")
                }
            }
        }
        lastVersion.set(raw.version)
        bootstrapped.set(true)
    }

    override fun refreshTtl(): Mono<*> = Mono.`when`(
        remoteMap.expire(ttl.toJavaDuration()),
        versionCounter.expire(ttl.toJavaDuration())
    )

    private fun putRemote(encKey: String, encValue: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_PUT,
            RScript.ReturnType.LONG,
            listOf(dataKey, createdChannel, updatedChannel, versionKey),
            instanceId,
            RedisCommands.PUBLISH.name,
            DELIM,
            encKey,
            encValue
        )
    }

    private fun removeRemote(encKey: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE,
            RScript.ReturnType.LONG,
            listOf(dataKey, removedChannel, versionKey),
            instanceId,
            RedisCommands.PUBLISH.name,
            DELIM,
            encKey
        )
    }

    private fun removeManyRemote(encKeys: List<String>): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE_MANY,
            RScript.ReturnType.LONG,
            listOf(dataKey, removedChannel, versionKey),
            instanceId,
            RedisCommands.PUBLISH.name,
            DELIM,
            *encKeys.toTypedArray()
        )
    }

    private fun clearRemote(): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_CLEAR,
            RScript.ReturnType.LONG,
            listOf(dataKey, clearedChannel, versionKey),
            instanceId,
            RedisCommands.PUBLISH.name,
            DELIM
        )
    }

    private fun onCreated(msg: String) {
        val parts = splitParts(msg, 4) ?: return // ver, origin, encKey, encVal
        val ver = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val encKey = parts[2]
        val encVal = parts[3]

        if (!applyVersion(ver)) return
        if (origin == instanceId) return

        val k = runCatching { decodeKey(encKey) }.getOrElse { loadFromRemote().subscribe(); return }
        val v = runCatching { decodeValue(encVal) }.getOrElse { loadFromRemote().subscribe(); return }

        val ok = lock.write { map.put(k, v) == null }
        if (!ok) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncMapChange.Put(k, v, null))
    }

    private fun onUpdated(msg: String) {
        val parts = splitParts(msg, 5) ?: return // ver, origin, encKey, new, old
        val ver = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val encKey = parts[2]
        val newEnc = parts[3]
        val oldEnc = parts[4]

        if (!applyVersion(ver)) return
        if (origin == instanceId) return

        val k = runCatching { decodeKey(encKey) }.getOrElse { loadFromRemote().subscribe(); return }
        val newVal = runCatching { decodeValue(newEnc) }.getOrElse { loadFromRemote().subscribe(); return }
        val oldVal = runCatching { decodeValue(oldEnc) }.getOrElse { loadFromRemote().subscribe(); return }

        val ok = lock.write {
            val cur = map[k] ?: return@write false
            if (cur != oldVal) return@write false
            map[k] = newVal
            true
        }
        if (!ok) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncMapChange.Put(k, newVal, oldVal))
    }

    private fun onRemoved(msg: String) {
        val parts = splitParts(msg, 4) ?: return // ver, origin, encKey, old
        val ver = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val encKey = parts[2]
        val oldEnc = parts[3]

        if (!applyVersion(ver)) return
        if (origin == instanceId) return

        val k = runCatching { decodeKey(encKey) }.getOrElse { loadFromRemote().subscribe(); return }
        val oldVal = runCatching { decodeValue(oldEnc) }.getOrElse { loadFromRemote().subscribe(); return }

        val ok = lock.write {
            val cur = map[k] ?: return@write false
            if (cur != oldVal) return@write false
            map.remove(k)
            true
        }
        if (!ok) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncMapChange.Removed(k, oldVal))
    }

    private fun onCleared(msg: String) {
        val parts = splitParts(msg, 2) ?: return // ver, origin
        val ver = parts[0].toLongOrNull() ?: return
        val origin = parts[1]

        if (!applyVersion(ver)) return
        if (origin == instanceId) return

        val had = lock.write {
            val h = map.isNotEmpty()
            map.clear()
            h
        }
        if (had) notifyListeners(SyncMapChange.Cleared())
    }

    private fun onKeyGone() {
        val had = lock.write {
            val h = map.isNotEmpty()
            map.clear()
            h
        }
        versionCounter.get().subscribe(
            { v -> lastVersion.set(v) },
            { /* ignore */ }
        )
        if (had) notifyListeners(SyncMapChange.Cleared())
    }

    private fun applyVersion(ver: Long): Boolean {
        if (!bootstrapped.get()) {
            loadFromRemote().subscribe()
            return false
        }
        val current = lastVersion.get()
        return when {
            ver <= current -> false
            ver == current + 1 -> {
                lastVersion.set(ver)
                true
            }

            else -> {
                loadFromRemote().subscribe()
                false
            }
        }
    }

    private fun splitParts(msg: String, n: Int): List<String>? {
        val out = ArrayList<String>(n)
        var start = 0
        for (k in 1 until n) {
            val i = msg.indexOf(DELIM, start)
            if (i < 0) return null
            out.add(msg.substring(start, i))
            start = i + 1
        }
        if (start > msg.length) return null
        out.add(msg.substring(start))
        return if (out.size == n) out else null
    }


    private fun encodeKey(key: K): String = api.json.encodeToString(keySerializer, key)
    private fun decodeKey(raw: String): K = api.json.decodeFromString(keySerializer, raw)
    private fun encodeValue(value: V): String = api.json.encodeToString(valueSerializer, value)
    private fun decodeValue(raw: String): V = api.json.decodeFromString(valueSerializer, raw)

    data class Snapshot(
        val entries: Map<String, String>,
        val version: Long
    ) {
        companion object {
            fun fromTuple(t: Tuple2<Map<String, String>, Long>) = Snapshot(t.t1, t.t2)
        }
    }
}