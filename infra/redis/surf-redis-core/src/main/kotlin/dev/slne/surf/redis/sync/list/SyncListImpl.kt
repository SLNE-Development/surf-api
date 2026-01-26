package dev.slne.surf.redis.sync.list

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractStreamSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure.SimpleVersionedSnapshot
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.redis.util.RedisExpirableUtils
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.serialization.KSerializer
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import java.util.*
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class SyncListImpl<T : Any>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    private val elementSerializer: KSerializer<T>
) : AbstractStreamSyncStructure<SyncListChange<T>, SimpleVersionedSnapshot<List<String>>>(
    api,
    id,
    ttl,
    Scripts,
    NAMESPACE
), SyncList<T> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "list:"

        private const val EVENT_ADDED = "A"
        private const val EVENT_REMOVED = "R"
        private const val EVENT_REMOVED_AT = "RA"
        private const val EVENT_SET_AT = "S"
        private const val EVENT_CLEARED = "C"

        private const val APPEND_SCRIPT = "append"
        private const val REMOVE_FIRST_SCRIPT = "remove-first"
        private const val REMOVE_AT_SCRIPT = "remove-at"
        private const val SET_AT_SCRIPT = "set-at"
        private const val REMOVE_MANY_SCRIPT = "remove-many"
        private const val CLEAR_SCRIPT = "clear"

        private object Scripts : LuaScriptRegistry("lua/sync/list") {
            init {
                load(APPEND_SCRIPT)
                load(REMOVE_FIRST_SCRIPT)
                load(REMOVE_AT_SCRIPT)
                load(SET_AT_SCRIPT)
                load(REMOVE_MANY_SCRIPT)
                load(CLEAR_SCRIPT)
            }
        }
    }


    private val list = ObjectArrayList<T>()
    private val remoteList by lazy { api.redissonReactive.getList<String>(dataKey, StringCodec.INSTANCE) }

    override fun init(): Mono<Void> {
        return super.init()
            .doOnSuccess {
                trackDisposable(RedisExpirableUtils.refreshContinuously(ttl, remoteList))
            }
            .then()
    }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        remoteList.addListener(DeletedObjectListener { requestResync() }),
        remoteList.addListener(ExpiredObjectListener { requestResync() })
    )

    override fun unregisterListener(id: Int): Mono<*> = remoteList.removeListener(id)

    override fun snapshot() = lock.read { ObjectArrayList(list) }
    override fun size(): Int = lock.read { list.size }
    override fun get(index: Int): T = lock.read { list[index] }
    override fun contains(element: T): Boolean = lock.read { list.contains(element) }

    override fun add(element: T) {
        val encoded = encodeValue(element)
        lock.write { list.add(element) }

        notifyListeners(SyncListChange.Appended(element))
        appendRemote(encoded)
    }

    override fun remove(element: T): Boolean {
        val removedLocal = lock.write { list.remove(element) }
        if (!removedLocal) return false

        val encoded = encodeValue(element)

        notifyListeners(SyncListChange.Removed(element))
        removeFirstRemote(encoded)

        return true
    }

    override fun removeAt(index: Int): T {
        val old = lock.write { list.removeAt(index) }

        notifyListeners(SyncListChange.RemovedAt(index, old))
        removeAtRemote(index)

        return old
    }

    override fun set(index: Int, element: T): T {
        val old = lock.write { list.set(index, element) }

        notifyListeners(SyncListChange.Updated(index, element, old))
        setAtRemote(index, encodeValue(element))

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
        removeManyRemote(encodedValues)

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
        clearRemote()
    }

    override fun loadFromRemote0(): Mono<SimpleVersionedSnapshot<List<String>>> = Mono.zip(
        remoteList.readAll(),
        versionCounter.get().onErrorReturn(0L)
    ).map { SimpleVersionedSnapshot.fromTuple(it) }


    override fun overrideFromRemote(raw: SimpleVersionedSnapshot<List<String>>) {
        val elements = raw.value.map(::decodeValue)
        lock.write {
            list.clear()
            list.addAll(elements)
        }
        super.overrideFromRemote(raw)
    }

    private fun appendRemote(encoded: String) {
        writeToRemote(APPEND_SCRIPT, EVENT_ADDED, encoded)
    }

    private fun removeFirstRemote(encoded: String) {
        writeToRemote(REMOVE_FIRST_SCRIPT, EVENT_REMOVED, encoded)
    }

    private fun removeAtRemote(index: Int) {
        val tombstone = "\u0001rm@" + UUID.randomUUID().toString()
        writeToRemote(REMOVE_AT_SCRIPT, EVENT_REMOVED_AT, index.toString(), tombstone)
    }

    private fun setAtRemote(index: Int, newEncoded: String) {
        writeToRemote(SET_AT_SCRIPT, EVENT_SET_AT, index.toString(), newEncoded)
    }

    private fun removeManyRemote(encodedValues: List<String>) {
        writeToRemote(REMOVE_MANY_SCRIPT, EVENT_REMOVED, *encodedValues.toTypedArray())
    }

    private fun clearRemote() {
        writeToRemote(CLEAR_SCRIPT, EVENT_CLEARED)
    }

    override fun onStreamEvent(type: String, data: StreamEventData) = when (type) {
        EVENT_ADDED -> onAdded(data)
        EVENT_REMOVED -> onRemoved(data)
        EVENT_REMOVED_AT -> onRemovedAt(data)
        EVENT_SET_AT -> onSetAt(data)
        EVENT_CLEARED -> onCleared(data)
        else -> log.atWarning().log("Unknown message type '$type' received from SyncList '$id'")
    }

    private fun onAdded(data: StreamEventData) {
        val idx = data.payload[0].toIntOrNull() ?: return
        val encoded = data.payload[1]

        val element = decodeValue(encoded)
        val ok = lock.write {
            if (idx < 0 || idx > list.size) return@write false
            list.add(idx, element)
            true
        }

        if (!ok) return requestResync()
        notifyListeners(SyncListChange.Added(idx, element))
    }

    private fun onRemoved(data: StreamEventData) {
        val encoded = data.payload[0]

        val element = decodeValue(encoded)
        val removed = lock.write { list.remove(element) }

        if (!removed) return requestResync()
        notifyListeners(SyncListChange.Removed(element))
    }

    private fun onRemovedAt(data: StreamEventData) {
        val idx = data.payload[0].toIntOrNull() ?: return
        val oldEncoded = data.payload[1]

        val old = decodeValue(oldEncoded)
        val ok = lock.write {
            if (idx < 0 || idx >= list.size) return@write false
            val cur = list[idx]
            if (cur != old) return@write false
            list.removeAt(idx)
            true
        }

        if (!ok) return requestResync()
        notifyListeners(SyncListChange.Removed(old))
    }

    private fun onSetAt(data: StreamEventData) {
        val idx = data.payload[0].toIntOrNull() ?: return
        val oldEnc = data.payload[1]
        val newEnc = data.payload[2]

        val newVal = decodeValue(newEnc)
        val oldVal = decodeValue(oldEnc)

        val ok = lock.write {
            if (idx < 0 || idx >= list.size) return@write false
            val cur = list[idx]
            if (cur != oldVal) return@write false
            list[idx] = newVal
            true
        }

        if (!ok) return requestResync()
        notifyListeners(SyncListChange.Updated(idx, newVal, oldVal))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCleared(data: StreamEventData) {
        val had = lock.write {
            val h = list.isNotEmpty()
            list.clear()
            h
        }

        if (had) {
            notifyListeners(SyncListChange.Cleared())
        }
    }

    private fun encodeValue(value: T) = api.json.encodeToString(elementSerializer, value)
    private fun decodeValue(value: String) = api.json.decodeFromString(elementSerializer, value)
}