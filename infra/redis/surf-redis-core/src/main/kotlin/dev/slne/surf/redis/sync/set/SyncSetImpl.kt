package dev.slne.surf.redis.sync.set

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractStreamSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure.SimpleVersionedSnapshot
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.redis.util.RedisExpirableUtils
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.serialization.KSerializer
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class SyncSetImpl<T : Any>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    private val elementSerializer: KSerializer<T>
) : AbstractStreamSyncStructure<SyncSetChange, SimpleVersionedSnapshot<Set<String>>>(api, id, ttl, Scripts, NAMESPACE),
    SyncSet<T> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "set:"

        private const val EVENT_ADDED = "A"
        private const val EVENT_REMOVED = "R"
        private const val EVENT_CLEARED = "C"

        private const val ADD_SCRIPT = "add"
        private const val REMOVE_SCRIPT = "remove"
        private const val REMOVE_MANY_SCRIPT = "remove-many"
        private const val CLEAR_SCRIPT = "clear"

        private object Scripts : LuaScriptRegistry("lua/sync/set") {
            init {
                load(ADD_SCRIPT)
                load(REMOVE_SCRIPT)
                load(REMOVE_MANY_SCRIPT)
                load(CLEAR_SCRIPT)
            }
        }
    }

    private val set = ObjectOpenHashSet<T>()

    private val remoteSet by lazy { api.redissonReactive.getSet<String>(dataKey, StringCodec.INSTANCE) }

    override fun init(): Mono<Void> {
        return super.init()
            .doOnSuccess {
                trackDisposable(RedisExpirableUtils.refreshContinuously(ttl, remoteSet))
            }
            .then()
    }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        remoteSet.addListener(DeletedObjectListener { requestResync() }),
        remoteSet.addListener(ExpiredObjectListener { requestResync() })
    )

    override fun unregisterListener(id: Int): Mono<*> = remoteSet.removeListener(id)

    override fun snapshot() = lock.read { ObjectOpenHashSet(set) }
    override fun size() = lock.read { set.size }
    override fun contains(element: T) = lock.read { set.contains(element) }

    override fun add(element: T): Boolean {
        val added = lock.write { set.add(element) }
        if (!added) return false

        addRemote(element)
        notifyListeners(SyncSetChange.Added(element))

        return true
    }

    override fun remove(element: T): Boolean {
        val removed = lock.write { set.remove(element) }
        if (!removed) return false

        removeRemote(element)
        notifyListeners(SyncSetChange.Removed(element))
        return true
    }

    override fun clear() {
        val hadElements = lock.write {
            val had = set.isNotEmpty()
            set.clear()
            had
        }
        if (!hadElements) return

        clearRemote()
        notifyListeners(SyncSetChange.Cleared)
    }

    override fun removeIf(predicate: (T) -> Boolean): Boolean {
        val removedElements = lock.write {
            val removed = ObjectOpenHashSet<T>()
            val iterator = set.iterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (predicate(element)) {
                    iterator.remove()
                    removed.add(element)
                }
            }

            removed
        }

        if (removedElements.isEmpty()) return false

        val encoded = removedElements.map { encodeValue(it) }

        removeRemoteMany(encoded)
        removedElements.forEach { element ->
            notifyListeners(SyncSetChange.Removed(element))
        }
        return true
    }

    private fun addRemote(element: T) {
        writeToRemote(ADD_SCRIPT, EVENT_ADDED, encodeValue(element))
    }

    private fun removeRemote(element: T) {
        writeToRemote(REMOVE_SCRIPT, EVENT_REMOVED, encodeValue(element))
    }

    private fun removeRemoteMany(encodedValues: List<String>) {
        writeToRemote(REMOVE_MANY_SCRIPT, EVENT_REMOVED, *encodedValues.toTypedArray())
    }

    private fun clearRemote() {
        writeToRemote(CLEAR_SCRIPT, EVENT_CLEARED)
    }

    override fun onStreamEvent(type: String, data: StreamEventData) = when (type) {
        EVENT_ADDED -> onAddedEvent(data)
        EVENT_REMOVED -> onDeletedEvent(data)
        EVENT_CLEARED -> onClearEvent(data)
        else -> log.atWarning().log("Unknown event type '$type' for SyncSet '$id'")
    }

    private fun onAddedEvent(data: StreamEventData) {
        val encoded = data.payload[0]
        val decoded = decodeValue(encoded)

        val added = lock.write { set.add(decoded) }

        if (!added) return requestResync()
        notifyListeners(SyncSetChange.Added(decoded))
    }

    private fun onDeletedEvent(data: StreamEventData) {
        val encoded = data.payload[0]
        val decoded = decodeValue(encoded)

        val removed = lock.write { set.remove(decoded) }

        if (!removed) return requestResync()
        notifyListeners(SyncSetChange.Removed(decoded))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClearEvent(data: StreamEventData) {
        val hadElements = lock.write {
            val had = set.isNotEmpty()
            set.clear()
            had
        }

        if (hadElements) {
            notifyListeners(SyncSetChange.Cleared)
        }
    }

    override fun loadFromRemote0(): Mono<SimpleVersionedSnapshot<Set<String>>> = Mono.zip(
        remoteSet.readAll(),
        versionCounter.get().onErrorReturn(0)
    ).map { SimpleVersionedSnapshot.fromTuple(it) }

    override fun overrideFromRemote(raw: SimpleVersionedSnapshot<Set<String>>) {
        val decoded = raw.value.map(::decodeValue)
        lock.write {
            set.clear()
            set.addAll(decoded)
        }
        super.overrideFromRemote(raw)
    }

    private fun encodeValue(value: T) = api.json.encodeToString(elementSerializer, value)
    private fun decodeValue(value: String) = api.json.decodeFromString(elementSerializer, value)
}