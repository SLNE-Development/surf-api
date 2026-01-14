package dev.slne.surf.redis.sync.list

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.surfapi.core.api.util.logger
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

class SyncListImpl<T : Any>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    private val elementSerializer: KSerializer<T>
) : AbstractSyncStructure<SyncListChange<T>, SyncListImpl.Snapshot>(api, id, ttl),
    SyncList<T> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "list:"
        private const val MSG_DELIMITER = "\u0000"

        private val instanceId = UUID.randomUUID().toString().replace("-", "")

        @Language("Lua")
        private val LUA_APPEND = """
            redis.call('RPUSH', KEYS[1], ARGV[1])
            local ver = redis.call('INCR', KEYS[3])
            local idx = redis.call('LLEN', KEYS[1]) - 1
            local msg = tostring(ver) .. ARGV[4] .. ARGV[2] .. ARGV[4] .. tostring(idx) .. ARGV[4] .. ARGV[1]
            redis.call(ARGV[3], KEYS[2], msg)
            return ver
        """.trimIndent()

        @Language("Lua")
        private val LUA_REMOVE_FIRST = """
            local removed = redis.call('LREM', KEYS[1], 1, ARGV[1])
            if removed == 1 then
                local ver = redis.call('INCR', KEYS[3])
                local msg = tostring(ver) .. ARGV[4] .. ARGV[2] .. ARGV[4] .. ARGV[1]
                redis.call(ARGV[3], KEYS[2], msg)
                return ver
            end
            return 0
        """.trimIndent()

        @Language("Lua")
        private val LUA_REMOVE_AT = """
            local idx = tonumber(ARGV[1])
            local old = redis.call('LINDEX', KEYS[1], idx)
            if old == false then
                return 0
            end
            redis.call('LSET', KEYS[1], idx, ARGV[2])
            redis.call('LREM', KEYS[1], 1, ARGV[2])
            local ver = redis.call('INCR', KEYS[3])
            local msg = tostring(ver) .. ARGV[5] .. ARGV[3] .. ARGV[5] .. tostring(idx) .. ARGV[5] .. old
            redis.call(ARGV[4], KEYS[2], msg)
            return ver
        """.trimIndent()

        @Language("Lua")
        private val LUA_SET_AT = """
            local idx = tonumber(ARGV[1])
            local old = redis.call('LINDEX', KEYS[1], idx)
            if old == false then
                return 0
            end
            redis.call('LSET', KEYS[1], idx, ARGV[2])
            local ver = redis.call('INCR', KEYS[3])
            local msg = tostring(ver) .. ARGV[5] .. ARGV[3] .. ARGV[5] .. tostring(idx) .. ARGV[5] .. ARGV[2] .. ARGV[5] .. old
            redis.call(ARGV[4], KEYS[2], msg)
            return ver
        """.trimIndent()

        @Language("Lua")
        private val LUA_REMOVE_MANY = """
            local removedCount = 0
            for i = 4, #ARGV do
                local removed = redis.call('LREM', KEYS[1], 1, ARGV[i])
                if removed == 1 then
                    removedCount = removedCount + 1
                    local ver = redis.call('INCR', KEYS[3])
                    local msg = tostring(ver) .. ARGV[3] .. ARGV[1] .. ARGV[3] .. ARGV[i]
                    redis.call(ARGV[2], KEYS[2], msg)
                end
            end
            return removedCount
        """.trimIndent()

        @Language("Lua")
        private val LUA_CLEAR = """
            local existed = redis.call('EXISTS', KEYS[1])
            redis.call('DEL', KEYS[1])
            if existed == 1 then
                local ver = redis.call('INCR', KEYS[3])
                local msg = tostring(ver) .. ARGV[3] .. ARGV[1]
                redis.call(ARGV[2], KEYS[2], msg)
                return ver
            end
            return 0
        """.trimIndent()
    }

    private val namespace = "$NAMESPACE$id:"
    private val dataKey = "${namespace}snapshot"
    private val versionKey = "${namespace}version"
    private val addedChannel = "${namespace}added"
    private val removedChannel = "${namespace}removed"
    private val removedAtChannel = "${namespace}removedAt"
    private val updatedChannel = "${namespace}updated"
    private val clearedChannel = "${namespace}cleared"

    private val list = ObjectArrayList<T>()

    private val remoteList by lazy { api.redissonReactive.getList<String>(dataKey, StringCodec.INSTANCE) }
    private val versionCounter by lazy { api.redissonReactive.getAtomicLong(versionKey) }
    private val script by lazy { api.redissonReactive.getScript(StringCodec.INSTANCE) }

    private val addedTopic by lazy { api.redissonReactive.getTopic(addedChannel, StringCodec.INSTANCE) }
    private val removedTopic by lazy { api.redissonReactive.getTopic(removedChannel, StringCodec.INSTANCE) }
    private val removedAtTopic by lazy { api.redissonReactive.getTopic(removedAtChannel, StringCodec.INSTANCE) }
    private val updatedTopic by lazy { api.redissonReactive.getTopic(updatedChannel, StringCodec.INSTANCE) }
    private val clearedTopic by lazy { api.redissonReactive.getTopic(clearedChannel, StringCodec.INSTANCE) }

    private val topicListenerIds = ConcurrentHashMap<String, MutableCollection<Int>>()

    private val lastVersion = AtomicLong(0L)
    private val bootstrapped = AtomicBoolean(false)

    override fun init(): Mono<Void> {
        return super.init()
            .then(addedTopic.addListener(String::class.java) { _, msg -> onAdded(msg) }
                .doOnNext { addTopicListenerId(addedChannel, it) })
            .then(removedTopic.addListener(String::class.java) { _, msg -> onRemoved(msg) }
                .doOnNext { addTopicListenerId(removedChannel, it) })
            .then(removedAtTopic.addListener(String::class.java) { _, msg -> onRemovedAt(msg) }
                .doOnNext { addTopicListenerId(removedAtChannel, it) })
            .then(updatedTopic.addListener(String::class.java) { _, msg -> onUpdated(msg) }
                .doOnNext { addTopicListenerId(updatedChannel, it) })
            .then(clearedTopic.addListener(String::class.java) { _, msg -> onCleared(msg) }
                .doOnNext { addTopicListenerId(clearedChannel, it) })
            .then()
    }

    override fun dispose() {
        topicListenerIds[addedChannel]?.forEach { addedTopic.removeListener(it).subscribe() }
        topicListenerIds[removedChannel]?.forEach { removedTopic.removeListener(it).subscribe() }
        topicListenerIds[removedAtChannel]?.forEach { removedAtTopic.removeListener(it).subscribe() }
        topicListenerIds[updatedChannel]?.forEach { updatedTopic.removeListener(it).subscribe() }
        topicListenerIds[clearedChannel]?.forEach { clearedTopic.removeListener(it).subscribe() }
        topicListenerIds.clear()
        super.dispose()
    }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        remoteList.addListener(DeletedObjectListener { onKeyGone() }),
        remoteList.addListener(ExpiredObjectListener { onKeyGone() })
    )

    override fun unregisterListener(id: Int): Mono<*> = remoteList.removeListener(id)

    private fun addTopicListenerId(name: String, id: Int) {
        val ids = topicListenerIds.computeIfAbsent(name) { ConcurrentHashMap.newKeySet() }
        ids.add(id)
    }

    override fun snapshot() = lock.read { ObjectArrayList(list) }
    override fun size(): Int = lock.read { list.size }
    override fun get(index: Int): T = lock.read { list[index] }
    override fun contains(element: T): Boolean = lock.read { list.contains(element) }

    override fun add(element: T) {
        val encoded = encodeValue(element)

        lock.write { list.add(element) }
        notifyListeners(SyncListChange.Appended(element))

        appendRemote(encoded).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to append element to SyncList '$id'") }
        )
    }

    override fun remove(element: T): Boolean {
        val removedLocal = lock.write { list.remove(element) }
        if (!removedLocal) return false

        val encoded = encodeValue(element)
        notifyListeners(SyncListChange.Removed(element))

        removeFirstRemote(encoded).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to remove element from SyncList '$id'") }
        )

        return true
    }

    override fun removeAt(index: Int): T {
        val old = lock.write { list.removeAt(index) }

        notifyListeners(SyncListChange.RemovedAt(index, old))

        removeAtRemote(index).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to removeAt($index) from SyncList '$id'") }
        )

        return old
    }

    override fun set(index: Int, element: T): T {
        val old = lock.write { list.set(index, element) }

        notifyListeners(SyncListChange.Updated(index, element, old))

        setAtRemote(index, encodeValue(element)).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to set($index) in SyncList '$id'") }
        )

        return old
    }

    override fun removeIf(predicate: (T) -> Boolean): Boolean {
        val removedValues = lock.write {
            val removed = ObjectArrayList<T>()
            val it = list.iterator()
            while (it.hasNext()) {
                val v = it.next()
                if (predicate(v)) {
                    it.remove()
                    removed.add(v)
                }
            }
            removed
        }

        if (removedValues.isEmpty) return false

        removedValues.forEach { notifyListeners(SyncListChange.Removed(it)) }

        val encodedValues = removedValues.map(::encodeValue)
        removeManyRemote(encodedValues).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to removeIf() from SyncList '$id'") }
        )

        return true
    }


    override fun clear() {
        val had = lock.write {
            val h = list.isNotEmpty()
            list.clear()
            h
        }
        if (!had) return

        notifyListeners(SyncListChange.Cleared())

        clearRemote().subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to clear SyncList '$id'") }
        )
    }

    override fun loadFromRemote0(): Mono<Snapshot> = Mono.zip(
        remoteList.readAll(),
        versionCounter.get().onErrorReturn(0L)
    ).map(Snapshot::fromTuple)

    override fun overrideFromRemote(raw: Snapshot) {
        lock.write {
            list.clear()
            raw.elements.forEach { list.add(decodeValue(it)) }
        }
        lastVersion.set(raw.version)
        bootstrapped.set(true)
    }

    override fun refreshTtl(): Mono<*> = Mono.`when`(
        remoteList.expire(ttl.toJavaDuration()),
        versionCounter.expire(ttl.toJavaDuration())
    )

    private fun appendRemote(encoded: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_APPEND,
            RScript.ReturnType.LONG,
            listOf(dataKey, addedChannel, versionKey),
            encoded,
            instanceId,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER
        )
    }

    private fun removeFirstRemote(encoded: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE_FIRST,
            RScript.ReturnType.LONG,
            listOf(dataKey, removedChannel, versionKey),
            encoded,
            instanceId,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER
        )
    }

    private fun removeAtRemote(index: Int): Mono<Long> {
        val tombstone = "\u0001rm@" + UUID.randomUUID().toString()
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE_AT,
            RScript.ReturnType.LONG,
            listOf(dataKey, removedAtChannel, versionKey),
            index.toString(),
            tombstone,
            instanceId,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER
        )
    }

    private fun setAtRemote(index: Int, newEncoded: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_SET_AT,
            RScript.ReturnType.LONG,
            listOf(dataKey, updatedChannel, versionKey),
            index.toString(),
            newEncoded,
            instanceId,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER
        )
    }

    private fun removeManyRemote(encodedValues: List<String>): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE_MANY,
            RScript.ReturnType.LONG,
            listOf(dataKey, removedChannel, versionKey),
            instanceId,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER,
            *encodedValues.toTypedArray()
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
            MSG_DELIMITER
        )
    }

    private fun onAdded(msg: String) {
        val parts = splitParts(msg, 4) ?: return
        val ver = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val idx = parts[2].toIntOrNull() ?: return
        val encoded = parts[3]

        if (!applyVersion(ver)) return
        if (origin == instanceId) return

        val element = decodeValue(encoded)
        val ok = lock.write {
            if (idx < 0 || idx > list.size) return@write false
            list.add(idx, element)
            true
        }

        if (!ok) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncListChange.Added(idx, element))
    }

    private fun onRemoved(msg: String) {
        val parts = splitParts(msg, 3) ?: return
        val version = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val encoded = parts[2]

        if (!applyVersion(version)) return
        if (origin == instanceId) return

        val element = decodeValue(encoded)
        val removed = lock.write { list.remove(element) }
        if (!removed) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncListChange.Removed(element))
    }

    private fun onRemovedAt(msg: String) {
        val parts = splitParts(msg, 4) ?: return
        val version = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val idx = parts[2].toIntOrNull() ?: return
        val oldEncoded = parts[3]

        if (!applyVersion(version)) return
        if (origin == instanceId) return

        val old = decodeValue(oldEncoded)
        val ok = lock.write {
            if (idx < 0 || idx >= list.size) return@write false
            val cur = list[idx]
            if (cur != old) return@write false
            list.removeAt(idx)
            true
        }
        if (!ok) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncListChange.Removed(old))
    }

    private fun onUpdated(msg: String) {
        val parts = splitParts(msg, 5) ?: return
        val version = parts[0].toLongOrNull() ?: return
        val origin = parts[1]
        val idx = parts[2].toIntOrNull() ?: return
        val newEnc = parts[3]
        val oldEnc = parts[4]

        if (!applyVersion(version)) return
        if (origin == instanceId) return

        val newVal = decodeValue(newEnc)
        val oldVal = decodeValue(oldEnc)

        val ok = lock.write {
            if (idx < 0 || idx >= list.size) return@write false
            val cur = list[idx]
            if (cur != oldVal) return@write false
            list[idx] = newVal
            true
        }
        if (!ok) {
            loadFromRemote().subscribe()
            return
        }
        notifyListeners(SyncListChange.Updated(idx, newVal, oldVal))
    }

    private fun onCleared(msg: String) {
        val parts = splitParts(msg, 2) ?: return
        val ver = parts[0].toLongOrNull() ?: return
        val origin = parts[1]

        if (!applyVersion(ver)) return
        if (origin == instanceId) return

        val had = lock.write {
            val h = list.isNotEmpty()
            list.clear()
            h
        }

        if (had) {
            notifyListeners(SyncListChange.Cleared())
        }
    }

    private fun onKeyGone() {
        val had = lock.write {
            val h = list.isNotEmpty()
            list.clear()
            h
        }
        versionCounter.get().subscribe(
            { v -> lastVersion.set(v) },
            { /* ignore */ }
        )
        if (had) notifyListeners(SyncListChange.Cleared())
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
            val i = msg.indexOf(MSG_DELIMITER, start)
            if (i < 0) return null
            out.add(msg.substring(start, i))
            start = i + 1
        }
        if (start > msg.length) return null
        out.add(msg.substring(start))
        return if (out.size == n) out else null
    }


    private fun encodeValue(value: T) = api.json.encodeToString(elementSerializer, value)
    private fun decodeValue(value: String) = api.json.decodeFromString(elementSerializer, value)

    data class Snapshot(
        val elements: List<String>,
        val version: Long
    ) {
        companion object {
            fun fromTuple(tuple: Tuple2<List<String>, Long>) = Snapshot(tuple.t1, tuple.t2)
        }
    }
}