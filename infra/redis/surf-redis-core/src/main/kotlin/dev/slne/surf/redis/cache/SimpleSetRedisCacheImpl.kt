package dev.slne.surf.redis.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import org.redisson.api.RScript
import org.redisson.api.options.KeysScanOptions
import org.redisson.client.codec.StringCodec
import reactor.core.Disposable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Redis-backed indexed set cache with a near-cache (Caffeine).
 *
 * Redis layout (all keys share the same Redis Cluster hash slot using [namespace] tag):
 * - ids-set:   [namespace]:&#95;&#95;ids&#95;&#95; -> Set&lt;id>
 * - value:     [namespace]:&#95;&#95;val&#95;&#95;:&lt;id&gt; -> JSON string
 * - index-set: [namespace]:&#95;&#95;idx&#95;&#95;:&lt;indexName&gt;:&lt;indexValue&gt; -> Set&lt;id&gt;
 * - meta-set:  [namespace]:&#95;&#95;meta&#95;&#95;:&lt;id&gt;:&lt;indexName&gt; -> Set&lt;indexValue&gt;
 *
 * Why meta-set?
 * - Allows atomic index diffs in Lua without loading the previous entity on the client.
 */
class SimpleSetRedisCacheImpl<T : Any>(
    private val namespace: String,
    private val serializer: KSerializer<T>,
    private val idOf: (T) -> String,
    private val indexes: RedisSetIndexes<T> = RedisSetIndexes.empty(),
    private val ttl: Duration,
    private val api: RedisApi,
    nearValueCacheSize: Long = 10_000,
    nearIndexCacheSize: Long = 50_000,
) : SimpleSetRedisCache<T> {
    private companion object {
        private val log = logger()

        private const val INVALIDATION_TOPIC_SUFFIX = ":__cache_invalidate__"
        private const val MESSAGE_DELIMITER = '\u0000'

        private const val OP_ALL = "ALL"
        private const val OP_VAL = "VAL"
        private const val OP_IDS = "IDS"
        private const val OP_IDX = "IDX"

        private const val IDS_LOCAL_KEY = "__ids__"

        private val nodeId = UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * Represents the default Time-To-Live (TTL) duration for negative cache entries, i.e.,
     * entries created to temporarily store the absence of a value in the Redis set.
     *
     * The value is computed as the smaller of 10% of the primary TTL value (`ttl / 10`)
     * or a fixed duration of 5 seconds, with a lower bound of 250 milliseconds imposed
     * to prevent excessively short durations.
     *
     * This property helps to manage the caching of missed lookups, ensuring that such
     * entries are not retained for an unnecessarily long period while minimizing excessive
     * retries or cache churn in high-traffic scenarios.
     */
    private val negativeTtl = minOf(ttl / 10, 5.seconds).coerceAtLeast(250.milliseconds)

    private val slotTag = "{$namespace}"
    private val keyPrefix = "$namespace:$slotTag"

    private val invalidationTopicName = "$namespace$INVALIDATION_TOPIC_SUFFIX"
    private val invalidationTopic by lazy {
        api.redissonReactive.getTopic(invalidationTopicName, StringCodec.INSTANCE)
    }

    private var invalidationSubscriptionDisposable: Disposable? = null

    @Volatile
    private var subscribed: Boolean = false

    @Volatile
    private var disposed: Boolean = false

    private val script by lazy { api.redissonReactive.getScript(StringCodec.INSTANCE) }

    private val nearValues = buildNearCache<T>(nearValueCacheSize)
    private val nearIds = buildNearCache<Set<String>>(1)
    private val nearIndexIds = buildNearCache<Set<String>>(nearIndexCacheSize)

    /**
     * Limits TTL refresh traffic (e.g. if a hot key is read very frequently).
     */
    private val refreshGate = Caffeine.newBuilder()
        .expireAfterWrite((ttl / 4).coerceIn(250.milliseconds, 1.hours))
        .maximumSize(100_000)
        .build<String, Unit>()


    private fun <V : Any> buildNearCache(maxSize: Long) = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfter(object : Expiry<String, CacheEntry<V>> {
            private fun nanos(d: Duration): Long = d.inWholeNanoseconds.coerceAtLeast(1)

            override fun expireAfterCreate(
                key: String,
                value: CacheEntry<V>,
                currentTime: Long
            ): Long = when (value) {
                is CacheEntry.Value -> nanos(ttl)
                CacheEntry.Null -> nanos(negativeTtl)
            }

            override fun expireAfterUpdate(
                key: String,
                value: CacheEntry<V>,
                currentTime: Long,
                currentDuration: Long
            ): Long = expireAfterCreate(key, value, currentTime)

            override fun expireAfterRead(
                key: String,
                value: CacheEntry<V>,
                currentTime: Long,
                currentDuration: Long
            ): Long = when (value) {
                is CacheEntry.Value -> nanos(ttl)
                CacheEntry.Null -> currentDuration // keep negative entry TTL on read
            }

        })
        .build<String, CacheEntry<V>>()


    private fun ensureSubscribed() {
        if (disposed) error("Cache '$namespace' is disposed")
        if (subscribed) return

        synchronized(this) {
            if (disposed) error("Cache '$namespace' is disposed")
            if (subscribed) return

            invalidationSubscriptionDisposable = invalidationTopic.getMessages(String::class.java)
                .doOnSubscribe { subscribed = true }
                .doFinally { subscribed = false }
                .subscribe(
                    { message ->
                        val parts = message.split(MESSAGE_DELIMITER)
                        if (parts.size < 2) {
                            log.atWarning()
                                .log("Received malformed cache invalidation message on topic $invalidationTopicName")
                            return@subscribe
                        }

                        val publisherNodeId = parts[0]
                        if (publisherNodeId == nodeId) return@subscribe

                        when (parts[1]) {
                            OP_ALL -> {
                                clearNearCacheOnly()
                            }

                            OP_VAL -> {
                                val id = parts.getOrNull(2) ?: return@subscribe
                                nearValues.invalidate(id)
                                refreshGate.invalidate(refreshKeyVal(id))
                            }

                            OP_IDS -> {
                                nearIds.invalidate(IDS_LOCAL_KEY)
                                refreshGate.invalidate(refreshKeyIds())
                            }

                            OP_IDX -> {
                                val name = parts.getOrNull(2) ?: return@subscribe
                                val value = parts.getOrNull(3) ?: return@subscribe
                                nearIndexIds.invalidate(indexCacheKey(name, value))
                                refreshGate.invalidate(refreshKeyIdx(name, value))
                            }

                            else -> {
                                log.atWarning().log(
                                    "Received unknown cache invalidation op '${parts[1]}' on topic $invalidationTopicName"
                                )
                            }
                        }
                    },
                    { error ->
                        log.atSevere().withCause(error)
                            .log("Error in cache invalidation subscription for topic $invalidationTopicName")
                    }
                )
        }
    }

    override fun dispose() {
        synchronized(this) {
            disposed = true
            subscribed = false

            invalidationSubscriptionDisposable?.dispose()
            invalidationSubscriptionDisposable = null
        }
    }

    override fun isDisposed(): Boolean {
        return disposed
    }


    private fun idsRedisKey(): String = "$keyPrefix:__ids__"
    private fun valueRedisPrefix(): String = "$keyPrefix:__val__:"
    private fun valueRedisKey(id: String): String = "$keyPrefix:__val__:$id"
    private fun metaRedisKey(id: String, indexName: String): String = "$keyPrefix:__meta__:$id:$indexName"
    private fun indexRedisKey(indexName: String, indexValue: String): String =
        "$keyPrefix:__idx__:$indexName:$indexValue"

    private fun indexCacheKey(indexName: String, indexValue: String): String =
        "$indexName$MESSAGE_DELIMITER$indexValue"

    private fun refreshKeyVal(id: String): String = "$OP_VAL$MESSAGE_DELIMITER$id"
    private fun refreshKeyIds(): String = OP_IDS
    private fun refreshKeyIdx(indexName: String, indexValue: String): String =
        "$OP_IDX$MESSAGE_DELIMITER$indexName$MESSAGE_DELIMITER$indexValue"

    private suspend fun publish(vararg parts: String) {
        for (p in parts) require(!p.contains(MESSAGE_DELIMITER)) { "Invalidation part contains NUL char" }
        invalidationTopic.publish(parts.joinToString(MESSAGE_DELIMITER.toString())).awaitSingleOrNull()
    }

    private suspend fun publishAllInvalidation() = publish(nodeId, OP_ALL)
    private suspend fun publishIdsInvalidation() = publish(nodeId, OP_IDS)
    private suspend fun publishValueInvalidation(id: String) = publish(nodeId, OP_VAL, id)
    private suspend fun publishIndexInvalidation(indexName: String, indexValue: String) =
        publish(nodeId, OP_IDX, indexName, indexValue)

    private fun requireNoNul(s: String, label: String) {
        require(!s.contains(MESSAGE_DELIMITER)) { "$label must not contain NUL char" }
    }

    private fun refreshTtl(gateKey: String, action: () -> Unit) {
        val inserted = refreshGate.asMap().putIfAbsent(gateKey, Unit) == null
        if (!inserted) return
        action()
    }

    /**
     * Refreshes TTL for:
     * - the value key
     * - all meta keys (so index diffs stay correct)
     *
     * Index-set TTL is refreshed when you query an index (see [refreshIndexTtl]).
     */
    private fun refreshValueTtl(id: String) {
        val valueKey = valueRedisKey(id)
        refreshTtl(refreshKeyVal(id)) {
            val keys = mutableObjectSetOf<String>()
            keys.add(valueRedisKey(id))
            for (idx in indexes.all) {
                keys.add(metaRedisKey(id, idx.name))
            }

            val batch = api.redissonReactive.createBatch()
            for (key in keys) {
                batch.getBucket<String>(key, StringCodec.INSTANCE)
                    .expire(ttl.toJavaDuration())
                    .doOnError { e -> log.atWarning().withCause(e).log("Failed to refresh TTL for $key") }
                    .subscribe()
            }
            batch.execute()
                .subscribe(
                    { /* Success */ },
                    { e ->
                        log.atWarning().withCause(e).log("Failed to refresh TTL for $valueKey and meta keys")
                    }
                )
        }
    }

    private fun refreshIdsTtl() {
        val redisKey = idsRedisKey()
        refreshTtl(refreshKeyIds()) {
            api.redissonReactive.getSet<String>(redisKey, StringCodec.INSTANCE)
                .expire(ttl.toJavaDuration())
                .doOnError { e -> log.atWarning().withCause(e).log("Failed to refresh TTL for $redisKey") }
                .subscribe()
        }
    }

    private fun refreshIndexTtl(indexName: String, indexValue: String) {
        val redisKey = indexRedisKey(indexName, indexValue)
        refreshTtl(refreshKeyIdx(indexName, indexValue)) {
            api.redissonReactive.getSet<String>(redisKey, StringCodec.INSTANCE)
                .expire(ttl.toJavaDuration())
                .doOnError { e -> log.atWarning().withCause(e).log("Failed to refresh TTL for $redisKey") }
                .subscribe()
        }
    }

    override suspend fun getCachedById(id: String): T? {
        ensureSubscribed()

        val normId = id.trim()
        if (normId.isEmpty()) return null
        requireNoNul(normId, "id")

        when (val entry = nearValues.getIfPresent(normId)) {
            is CacheEntry.Value -> {
                refreshValueTtl(normId)
                return entry.value
            }

            CacheEntry.Null -> {
                refreshValueTtl(normId)
                return null
            }

            null -> Unit
        }

        val bucket = api.redissonReactive.getBucket<String>(valueRedisKey(normId), StringCodec.INSTANCE)
        val raw = bucket.get().awaitSingleOrNull() ?: run {
            nearValues.put(normId, CacheEntry.Null)
            return null
        }

        bucket.expire(ttl.toJavaDuration()).awaitSingleOrNull()

        val value = api.json.decodeFromString(serializer, raw)
        nearValues.put(normId, CacheEntry.Value(value))

        // Best-effort keep meta alive together with the value (in case meta TTL is close to expiring).
        refreshValueTtl(normId)

        return value
    }

    private suspend fun getAllIdsCached(): Set<String> {
        ensureSubscribed()

        when (val entry = nearIds.getIfPresent(IDS_LOCAL_KEY)) {
            is CacheEntry.Value -> {
                refreshIdsTtl()
                return entry.value
            }

            CacheEntry.Null -> {
                refreshIdsTtl()
                return emptySet()
            }

            null -> Unit
        }

        val redisKey = idsRedisKey()
        val idsSet = api.redissonReactive.getSet<String>(redisKey, StringCodec.INSTANCE)

        val exists = api.redissonReactive.keys.countExists(redisKey).awaitSingle() > 0L
        val ids = if (exists) {
            val loaded = idsSet.readAll().awaitSingleOrNull().orEmpty()
            idsSet.expire(ttl.toJavaDuration()).awaitSingleOrNull()
            loaded
        } else {
            // Fallback: ids key missing -> rebuild from value keys
            val prefix = valueRedisPrefix()
            val keys = api.redissonReactive.keys
                .getKeys(KeysScanOptions.defaults().pattern("$prefix*"))
                .collectList()
                .awaitSingle()

            val rebuilt = keys.asSequence()
                .filter { it.startsWith(prefix) }
                .map { it.substring(prefix.length) }
                .filter { it.isNotEmpty() }
                .toSet()

            if (rebuilt.isNotEmpty()) {
                for (id in rebuilt) idsSet.add(id).awaitSingleOrNull()
                idsSet.expire(ttl.toJavaDuration()).awaitSingleOrNull()
            }
            rebuilt
        }

        nearIds.put(IDS_LOCAL_KEY, if (ids.isEmpty()) CacheEntry.Null else CacheEntry.Value(ids))
        return ids
    }

    override suspend fun findCached(condition: (T) -> Boolean): Set<T> {
        ensureSubscribed()

        val ids = getAllIdsCached()
        if (ids.isEmpty()) return emptySet()

        val result = ConcurrentHashMap.newKeySet<T>()
        val idsRedis = api.redissonReactive.getSet<String>(idsRedisKey(), StringCodec.INSTANCE)

        coroutineScope {
            for (id in ids) {
                launch {
                    val value = getCachedById(id)
                    if (value == null) {
                        idsRedis.remove(id).awaitSingleOrNull() // stale
                        nearIds.invalidate(IDS_LOCAL_KEY)

                    } else if (condition(value)) {
                        result.add(value)
                    }
                }
            }
        }

        return result
    }

    override suspend fun <V : Any> findByIndexCached(index: RedisSetIndex<T, V>, value: V): Set<T> {
        ensureSubscribed()

        require(indexes.containsSameInstance(index)) {
            "Index '${index.name}' is not registered in this cache instance. " +
                    "Use the index object from the same RedisSetIndexes registry that you passed into the cache."
        }

        val queryValue = index.valueString(value)
        requireNoNul(index.name, "indexName")
        requireNoNul(queryValue, "indexValue")

        val cacheKey = indexCacheKey(index.name, queryValue)

        val loadedIds: Set<String> = when (val entry = nearIndexIds.getIfPresent(cacheKey)) {
            is CacheEntry.Value -> {
                refreshIndexTtl(index.name, queryValue)
                entry.value
            }

            CacheEntry.Null -> emptySet()
            null -> {
                val redisKey = indexRedisKey(index.name, queryValue)
                val set = api.redissonReactive.getSet<String>(redisKey, StringCodec.INSTANCE)
                val ids = set.readAll().awaitSingleOrNull().orEmpty()
                set.expire(ttl.toJavaDuration()).awaitSingleOrNull()

                nearIndexIds.put(cacheKey, if (ids.isEmpty()) CacheEntry.Null else CacheEntry.Value(ids))
                ids
            }
        }

        if (loadedIds.isEmpty()) return emptySet()

        val redisIndexKey = indexRedisKey(index.name, queryValue)
        val indexSet = api.redissonReactive.getSet<String>(redisIndexKey, StringCodec.INSTANCE)

        val result = ConcurrentHashMap.newKeySet<T>(loadedIds.size)
        val filteredIds = ConcurrentHashMap.newKeySet<String>(loadedIds.size)
        var changed = false

        coroutineScope {
            for (id in loadedIds) {
                launch {
                    val element = getCachedById(id)
                    if (element == null) {
                        indexSet.remove(id).awaitSingleOrNull()
                        changed = true
                    } else {
                        val actual = index.extractStrings(element)
                        if (queryValue !in actual) {
                            indexSet.remove(id).awaitSingleOrNull()
                            changed = true
                        } else {
                            filteredIds.add(id)
                            result.add(element)
                        }
                    }
                }
            }
        }

        if (changed) {
            nearIndexIds.put(cacheKey, if (filteredIds.isEmpty()) CacheEntry.Null else CacheEntry.Value(filteredIds))
        }

        return result
    }

    override suspend fun add(element: T): Boolean {
        ensureSubscribed()

        val ttlMillis = ttl.inWholeMilliseconds
        require(ttlMillis > 0) { "ttl must be > 0ms" }

        val id = idOf(element).trim()
        require(id.isNotEmpty()) { "idOf(element) returned blank id" }
        requireNoNul(id, "id")

        val raw = api.json.encodeToString(serializer, element)

        val argv = ObjectArrayList<Any>(32)
        argv += keyPrefix
        argv += id
        argv += raw
        argv += ttlMillis.toString()
        argv += indexes.all.size.toString()

        for (idx in indexes.all) {
            requireNoNul(idx.name, "indexName")

            val values = idx.extractStrings(element).toList()
            for (v in values) requireNoNul(v, "indexValue")

            argv += idx.name
            argv += values.size.toString()
            for (v in values) argv += v
        }

        val result = SimpleSetRedisCacheLuaScripts.execute<List<Any>>(
            script,
            RScript.Mode.READ_WRITE,
            SimpleSetRedisCacheLuaScripts.UPSERT,
            RScript.ReturnType.LIST,
            listOf(idsRedisKey()), // routing key (cluster-safe)
            *argv.toTypedArray()
        )

        val (wasNew, touched) = parseLuaFlagAndTouched(result)

        // Local near-cache updates
        nearValues.put(id, CacheEntry.Value(element))
        nearIds.invalidate(IDS_LOCAL_KEY)
        for ((idxName, idxValue) in touched) {
            nearIndexIds.invalidate(indexCacheKey(idxName, idxValue))
        }

        // Cross-node invalidation
        publishValueInvalidation(id)
        publishIdsInvalidation()
        for ((idxName, idxValue) in touched) publishIndexInvalidation(idxName, idxValue)

        return wasNew
    }

    override suspend fun findCachedOrLoad(condition: (T) -> Boolean, loader: suspend () -> T): T {
        findCached(condition).firstOrNull()?.let { return it }
        val loaded = loader()
        add(loaded)
        return loaded
    }

    override suspend fun <V : Any> findCachedByIndexOrLoad(
        index: RedisSetIndex<T, V>,
        value: V,
        loader: suspend () -> T
    ): T {
        val cached = findFirstByIndexCached(index, value)
        if (cached != null) return cached

        val loaded = loader()
        add(loaded)
        return loaded
    }

    override suspend fun findCachedOrLoadNullable(condition: (T) -> Boolean, loader: suspend () -> T?): T? {
        findCached(condition).firstOrNull()?.let { return it }
        val loaded = loader() ?: return null
        add(loaded)
        return loaded
    }

    override suspend fun <V : Any> findCachedByIndexOrLoadNullable(
        index: RedisSetIndex<T, V>,
        value: V,
        loader: suspend () -> T?
    ): T? {
        val cached = findFirstByIndexCached(index, value)
        if (cached != null) return cached

        val loaded = loader() ?: return null
        add(loaded)
        return loaded
    }

    override suspend fun removeIf(predicate: (T) -> Boolean): Boolean {
        ensureSubscribed()

        val ids = getAllIdsCached()
        if (ids.isEmpty()) return false

        var removedAny = false
        for (id in ids) {
            val value = getCachedById(id) ?: continue
            if (predicate(value)) {
                removedAny = true
                removeById(id)
            }
        }
        return removedAny
    }

    override suspend fun removeById(id: String): Boolean {
        ensureSubscribed()

        val normId = id.trim()
        if (normId.isEmpty()) return false
        requireNoNul(normId, "id")

        val argv = ObjectArrayList<Any>(8 + indexes.all.size)
        argv += keyPrefix
        argv += normId
        argv += indexes.all.size.toString()
        for (idx in indexes.all) {
            requireNoNul(idx.name, "indexName")
            argv += idx.name
        }

        val result = SimpleSetRedisCacheLuaScripts.execute<List<Any>>(
            script,
            RScript.Mode.READ_WRITE,
            SimpleSetRedisCacheLuaScripts.REMOVE_BY_ID,
            RScript.ReturnType.LIST,
            listOf(idsRedisKey()),
            *argv.toTypedArray()
        )

        val (removed, touched) = parseLuaFlagAndTouched(result)

        // Local near-cache updates
        nearValues.invalidate(normId)
        nearIds.invalidate(IDS_LOCAL_KEY)
        refreshGate.invalidate(refreshKeyVal(normId))
        for ((idxName, idxValue) in touched) {
            nearIndexIds.invalidate(indexCacheKey(idxName, idxValue))
        }

        // Cross-node invalidation
        if (removed) {
            publishValueInvalidation(normId)
            publishIdsInvalidation()
            for ((idxName, idxValue) in touched) publishIndexInvalidation(idxName, idxValue)
        }

        return removed
    }

    override suspend fun <V : Any> removeByIndex(index: RedisSetIndex<T, V>, value: V): Boolean {
        ensureSubscribed()

        require(indexes.containsSameInstance(index)) {
            "Index '${index.name}' is not registered in this cache instance. " +
                    "Use the index object from the same RedisSetIndexes registry that you passed into the cache."
        }

        val queryValue = index.valueString(value)
        requireNoNul(index.name, "indexName")
        requireNoNul(queryValue, "indexValue")

        val argv = ArrayList<Any>(8 + indexes.all.size)
        argv += keyPrefix
        argv += index.name
        argv += queryValue
        argv += indexes.all.size.toString()
        for (idx in indexes.all) {
            requireNoNul(idx.name, "indexName")
            argv += idx.name
        }

        val removedCount = SimpleSetRedisCacheLuaScripts.execute<Long>(
            script,
            RScript.Mode.READ_WRITE,
            SimpleSetRedisCacheLuaScripts.REMOVE_BY_INDEX,
            RScript.ReturnType.LONG,
            listOf(idsRedisKey()),
            *argv.toTypedArray()
        )

        if (removedCount <= 0L) return false

        // This potentially touched many ids + many index keys -> coarse invalidation is safest.
        clearNearCacheOnly()
        publishAllInvalidation()

        return true
    }

    override suspend fun invalidateAll(): Long {
        ensureSubscribed()

        val deleted = api.redissonReactive.keys.deleteByPattern("$keyPrefix:*").awaitSingle()

        clearNearCacheOnly()
        publishAllInvalidation()

        return deleted
    }

    override fun clearNearCacheOnly() {
        nearValues.invalidateAll()
        nearIds.invalidateAll()
        nearIndexIds.invalidateAll()
        refreshGate.invalidateAll()
    }

    private fun parseLuaFlagAndTouched(result: List<Any?>): Pair<Boolean, Set<Pair<String, String>>> {
        if (result.isEmpty()) return false to emptySet()

        val flag = when (val v = result[0]) {
            is Number -> v.toLong() == 1L
            else -> v?.toString()?.toLongOrNull() == 1L
        }

        val touched = LinkedHashSet<Pair<String, String>>()
        for (i in 1 until result.size) {
            val s = result[i]?.toString() ?: continue
            val parts = s.split(MESSAGE_DELIMITER, limit = 2)
            if (parts.size == 2) {
                touched.add(parts[0] to parts[1])
            }
        }

        return flag to touched
    }


    private sealed class CacheEntry<out V> {
        data class Value<V>(val value: V) : CacheEntry<V>()
        object Null : CacheEntry<Nothing>()
    }
}