package dev.slne.surf.redis.sync.set

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
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

class SyncSetImpl<T : Any>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    private val elementSerializer: KSerializer<T>
) : AbstractSyncStructure<SyncSetChange, SyncSetImpl.Snapshot>(api, id, ttl),
    SyncSet<T> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "set:"
        private const val MSG_DELIMITER = "\u0000"

        private val instanceId = UUID.randomUUID().toString().replace("-", "")

        /**
         * Lua script used to add an element to a Redis set and manage versioning and publishing changes.
         *
         * The script performs the following:
         * - Adds the specified element to a Redis set identified by the provided key (`KEYS[1]`).
         * - If the element is added successfully (not already present in the set), it:
         *   - Increments a version counter stored under the key (`KEYS[3]`).
         *   - Publishes a message with the updated version and element to a Redis channel (`KEYS[2]`)
         *     using the specified Redis command (`ARGV[2]`), message delimiter (`ARGV[3]`), and element payload (`ARGV[1]`).
         * - Returns the updated version number if the element is added successfully; otherwise, returns 0.
         *
         * KEYS:
         * - `KEYS[1]`: Redis key for the set to which the element is being added.
         * - `KEYS[2]`: Redis channel key where a change notification will be published.
         * - `KEYS[3]`: Redis key for the version counter to be incremented.
         *
         * ARGV:
         * - `ARGV[1]`: The element to be added to the set.
         * - `ARGV[2]`: Redis command used for publishing messages (e.g., `PUBLISH`).
         * - `ARGV[3]`: A delimiter used in constructing the message to be published.
         *
         * Returns:
         * - The new version number if the element is successfully added to the set.
         * - `0` if the element already exists in the set.
         */
        @Language("Lua")
        private val LUA_ADD = """
            local added = redis.call('SADD', KEYS[1], ARGV[1])
            if added == 1 then
                local ver = redis.call('INCR', KEYS[3])
                redis.call(ARGV[2], KEYS[2], tostring(ver) .. ARGV[3] .. ARGV[1])
                return ver
            end
            return 0
        """.trimIndent()

        /**
         * A Lua script used to remove an element from a Redis set and publish a notification for the removal
         * if the operation is successful. Additionally, increments a version counter to track changes.
         *
         * The script performs the following steps:
         * 1. Attempts to remove the specified element from the Redis set identified by the first key (KEYS[1]).
         * 2. If the element was successfully removed (result is 1), the script:
         *    - Increments the version counter stored at the third key (KEYS[3]).
         *    - Publishes a formatted change notification to the channel identified by the second key (KEYS[2]).
         *      This notification includes the new version, a delimiter, and the removed element.
         * 3. Returns the updated version number if the removal was successful; otherwise, returns 0.
         */
        @Language("Lua")
        private val LUA_REMOVE = """
            local removed = redis.call('SREM', KEYS[1], ARGV[1])
            if removed == 1 then
                local ver = redis.call('INCR', KEYS[3])
                redis.call(ARGV[2], KEYS[2], tostring(ver) .. ARGV[3] .. ARGV[1])
                return ver
            end
            return 0
        """.trimIndent()

        /**
         * A Lua script used to remove multiple elements from a Redis set and publish deletion events.
         *
         * The script iterates through a list of provided elements (`ARGV`), attempts to remove them
         * from the target Redis set (`KEYS[1]`), and increments a version counter (`KEYS[3]`)
         * whenever an element is successfully removed. For each successful removal, an event
         * is published to a specified channel (`KEYS[2]`) using the provided command (`ARGV[1]`)
         * with a message combining the updated version, a delimiter (`ARGV[2]`),
         * and the removed element's value.
         *
         * Returns the count of elements successfully removed.
         *
         * Redis Keys:
         * - `KEYS[1]` - The Redis set from which elements will be removed.
         * - `KEYS[2]` - The channel to which deletion events will be published.
         * - `KEYS[3]` - The key for the version counter.
         *
         * Arguments:
         * - `ARGV[1]` - The Redis command used to publish messages (e.g., `PUBLISH`).
         * - `ARGV[2]` - The delimiter used in formatted messages.
         * - `ARGV[3..n]` - The list of elements to remove from the set.
         */
        @Language("Lua")
        private val LUA_REMOVE_MANY = """
            local removedCount = 0
            for i = 3, #ARGV do
                local removed = redis.call('SREM', KEYS[1], ARGV[i])
                if removed == 1 then
                    removedCount = removedCount + 1
                    local ver = redis.call('INCR', KEYS[3])
                    redis.call(ARGV[1], KEYS[2], tostring(ver) .. ARGV[2] .. ARGV[i])
                end
            end
            return removedCount
        """.trimIndent()
    }

    private val namespace = "$NAMESPACE$id:"
    private val dataKey = "${namespace}snapshot"
    private val versionKey = "${namespace}version"
    private val createdChannelName = "${namespace}created"
    private val deletedChannelName = "${namespace}deleted"

    private val set = ObjectOpenHashSet<T>()

    private val remoteSet by lazy { api.redissonReactive.getSet<String>(dataKey, StringCodec.INSTANCE) }
    private val versionCounter by lazy { api.redissonReactive.getAtomicLong(versionKey) }
    private val createdTopic by lazy { api.redissonReactive.getTopic(createdChannelName, StringCodec.INSTANCE) }
    private val deletedTopic by lazy { api.redissonReactive.getTopic(deletedChannelName, StringCodec.INSTANCE) }
    private val script by lazy { api.redissonReactive.getScript(StringCodec.INSTANCE) }

    private val listeners = ConcurrentHashMap<String, MutableCollection<Int>>()

    private val lastVersion = AtomicLong(0L)
    private val bootstrapped = AtomicBoolean(false)

    override fun init(): Mono<Void> {
        val createdTopicListener = createdTopic.addListener(String::class.java) { _, msg ->
            onCreated(msg)
        }.doOnNext { addListenerId(createdChannelName, it) }

        val deletedTopicListener = deletedTopic.addListener(String::class.java) { _, msg ->
            onDeleted(msg)
        }.doOnNext { addListenerId(deletedChannelName, it) }


        return super.init()
            .then(createdTopicListener)
            .then(deletedTopicListener)
            .then()
    }

    override fun dispose() {
        listeners[createdChannelName]?.forEach { createdTopic.removeListener(it).subscribe() }
        listeners[deletedChannelName]?.forEach { deletedTopic.removeListener(it).subscribe() }
        listeners.clear()
        super.dispose()
    }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        remoteSet.addListener(DeletedObjectListener { onClear() }),
        remoteSet.addListener(ExpiredObjectListener { onClear() })
    )

    override fun unregisterListener(id: Int): Mono<*> = remoteSet.removeListener(id)

    private fun addListenerId(name: String, id: Int) {
        val ids = listeners.computeIfAbsent(name) { ConcurrentHashMap.newKeySet() }
        ids.add(id)
    }

    private fun onCreated(raw: String) {
        val (version, origin, rawEncoded) = unwrapVersioned(raw) ?: return

        if (!bootstrapped.get()) {
            loadFromRemote().subscribe()
            return
        }

        val current = lastVersion.get()
        when {
            version <= current -> return
            version == current + 1 -> {
                lastVersion.set(version)
                if (origin == instanceId) return
                val element = decodeValue(rawEncoded)
                if (addLocal(element)) notifyListeners(SyncSetChange.Added(element))
            }

            else -> loadFromRemote().subscribe() // gap
        }
    }

    private fun onDeleted(raw: String) {
        val (version, origin, rawEncoded) = unwrapVersioned(raw) ?: return

        if (!bootstrapped.get()) {
            loadFromRemote().subscribe()
            return
        }

        val current = lastVersion.get()

        when {
            version <= current -> return
            version == current + 1 -> {
                lastVersion.set(version)
                if (origin == instanceId) return
                val element = decodeValue(rawEncoded)
                if (removeLocal(element)) notifyListeners(SyncSetChange.Removed(element))
            }

            else -> loadFromRemote().subscribe() // gap
        }
    }

    private fun onClear() {
        val hadElements = lock.write {
            val had = set.isNotEmpty()
            set.clear()
            had
        }
        if (hadElements) {
            notifyListeners(SyncSetChange.Cleared)
        }
    }

    override fun snapshot() = lock.read { ObjectOpenHashSet(set) }
    override fun size() = lock.read { set.size }
    override fun contains(element: T) = lock.read { set.contains(element) }

    override fun add(element: T): Boolean {
        val added = addLocal(element)
        if (!added) return false

        val encoded = encodeValue(element)
        addRemote(wrap(encoded)).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to add element '$element' to SyncSet '$id'") }
        )

        notifyListeners(SyncSetChange.Added(element))

        return true
    }

    private fun addLocal(element: T) = lock.write { set.add(element) }

    private fun addRemote(wrappedPayload: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_ADD,
            RScript.ReturnType.LONG,
            listOf(dataKey, createdChannelName, versionKey),
            wrappedPayload,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER
        )
    }

    override fun remove(element: T): Boolean {
        val removed = removeLocal(element)
        if (!removed) return false

        val encoded = encodeValue(element)
        removeRemote(wrap(encoded)).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("Failed to remove element '$element' from SyncSet '$id'") }
        )

        notifyListeners(SyncSetChange.Removed(element))
        return true
    }

    private fun removeLocal(element: T): Boolean {
        return lock.write { set.remove(element) }
    }

    private fun removeRemote(wrappedPayload: String): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE,
            RScript.ReturnType.LONG,
            listOf(dataKey, deletedChannelName, versionKey),
            wrappedPayload,
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER
        )
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

        val encoded = removedElements.map { wrap(encodeValue(it)) }

        removeRemoteMany(encoded).subscribe(
            { /* Success */ },
            { e -> log.atSevere().withCause(e).log("removeIf failed for SyncSet '$id'") }
        )

        removedElements.forEach { element ->
            notifyListeners(SyncSetChange.Removed(element))
        }
        return true
    }

    private fun removeRemoteMany(encodedValues: List<String>): Mono<Long> {
        return script.eval(
            dataKey,
            RScript.Mode.READ_WRITE,
            LUA_REMOVE_MANY,
            RScript.ReturnType.LONG,
            listOf(dataKey, deletedChannelName, versionKey),
            RedisCommands.PUBLISH.name,
            MSG_DELIMITER,
            *encodedValues.toTypedArray()
        )
    }

    override fun clear() {
        val hadElements = lock.write {
            val had = set.isNotEmpty()
            set.clear()
            had
        }
        if (!hadElements) return

        remoteSet.delete()
            .subscribe(
                { /* Success */ },
                { e ->
                    log.atSevere()
                        .withCause(e)
                        .log("Failed to clear SyncSet '$id'")
                }
            )

        notifyListeners(SyncSetChange.Cleared)
    }

    override fun loadFromRemote0(): Mono<Snapshot> = Mono.zip(
        remoteSet.readAll(),
        versionCounter.get().onErrorReturn(0)
    ).map(Snapshot::fromTuple)

    override fun overrideFromRemote(raw: Snapshot) {
        lock.write {
            set.clear()
            set.addAll(raw.elements.map(::decodeValue))
        }
        lastVersion.set(raw.version)
        bootstrapped.set(true)
    }

    override fun refreshTtl(): Mono<*> = Mono.`when`(
        remoteSet.expire(ttl.toJavaDuration()),
        versionCounter.expire(ttl.toJavaDuration())
    )

    private fun encodeValue(value: T) = api.json.encodeToString(elementSerializer, value)
    private fun decodeValue(value: String) = api.json.decodeFromString(elementSerializer, value)

    private fun wrap(value: String) = "$instanceId$MSG_DELIMITER$value"
    private fun unwrapPayload(payload: String): Pair<String, String>? {
        val i = payload.indexOf(MSG_DELIMITER)
        if (i <= 0 || i == payload.length - 1) return null
        return payload.substring(0, i) to payload.substring(i + 1)
    }

    private fun unwrapVersioned(msg: String): Triple<Long, String, String>? {
        val i = msg.indexOf(MSG_DELIMITER)
        if (i <= 0 || i == msg.length - 1) return null

        val ver = msg.substring(0, i).toLongOrNull() ?: return null
        val payload = msg.substring(i + 1)

        val (origin, encoded) = unwrapPayload(payload) ?: return null
        return Triple(ver, origin, encoded)
    }

    data class Snapshot(
        val elements: Set<String>,
        val version: Long
    ) {
        companion object {
            fun fromTuple(tuple: Tuple2<Set<String>, Long>) = Snapshot(tuple.t1, tuple.t2)
        }
    }
}