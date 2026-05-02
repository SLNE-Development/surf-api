package dev.slne.surf.redis.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.KSerializer
import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RScript
import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import org.redisson.client.codec.StringCodec
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration


class SimpleSetRedisCacheImpl<T : Any>(
    n: String,
    private val serializer: KSerializer<T>,
    private val idOf: (T) -> String,
    private val indexes: RedisSetIndexes<T> = RedisSetIndexes.empty(),
    private val ttl: Duration,
    private val api: RedisApi,
    nearValueCacheSize: Long = 10_000,
    nearIndexCacheSize: Long = 50_000,
) : DisposableAware(), SimpleSetRedisCache<T> {
    private companion object {
        private val log = logger()

        const val STREAM_FIELD_TYPE = "T"
        const val STREAM_FIELD_MSG = "M"

        private const val STREAM_SUFFIX = ":__stream__"
        private const val VERSION_KEY_SUFFIX = ":__version__"
        private const val IDS_KEY_SUFFIX = ":__ids__"
        private const val VALUE_KEY_INFIX = ":__val__:"
        private const val INDEX_KEY_INFIX = ":__idx__:"
        private const val NULL_MARKER = "__NULL__"
        private const val LOCAL_IDS_CACHE_KEY = "__ids__"
        private const val MAX_CONCURRENT_REDIS_OPS = 64
        private const val MESSAGE_DELIMITER = '\u0000'
        private const val STREAM_MAX_LENGTH = 10_000

        private const val OP_ALL = "A"
        private const val OP_VAL = "V"
        private const val OP_IDS = "IS"
        private const val OP_IDX = "IX"

        private const val UPSERT_SCRIPT = "upsert"
        private const val REMOVE_ID_SCRIPT = "remove_by_id"
        private const val REMOVE_INDEX_SCRIPT = "remove_by_index"
        private const val CLEAR_SCRIPT = "clear"
        private const val TOUCH_SCRIPT = "touch_value"

        private object LuaScripts : LuaScriptRegistry("lua/cache/simple-indexed") {
            init {
                load(UPSERT_SCRIPT)
                load(REMOVE_ID_SCRIPT)
                load(REMOVE_INDEX_SCRIPT)
                load(CLEAR_SCRIPT)
                load(TOUCH_SCRIPT)
            }
        }


        private fun requireNoNul(s: String, label: String) {
            require(!s.contains(MESSAGE_DELIMITER)) { "$label must not contain NUL char" }
        }
    }

    init {
        require(n.isNotBlank()) { "namespace must not be blank" }
        requireNoNul(n, "namespace")
        require(!n.contains('{') && !n.contains('}')) {
            "namespace must not contain '{' or '}' (used for Redis Cluster hash tags)"
        }
    }

    private val namespace = n.replace(":", "_")
    private val slotTag = "{$namespace}"
    private val keyPrefix = "$namespace:$slotTag"

    private val idsRedisKey = "$keyPrefix$IDS_KEY_SUFFIX"
    private fun valueRedisKey(id: String) = "$keyPrefix$VALUE_KEY_INFIX$id"
    private fun indexRedisKey(indexName: String, indexValue: String) =
        "$keyPrefix$INDEX_KEY_INFIX$indexName:$indexValue"

    private val streamKey = "$keyPrefix$STREAM_SUFFIX"
    private val versionKey = "$keyPrefix$VERSION_KEY_SUFFIX"

    private val scriptExecutor = LuaScriptExecutor.getInstance(api, LuaScripts)
    private val stream: RStreamReactive<String, String> by lazy {
        api.redissonReactive.getStream(streamKey, StringCodec.INSTANCE)
    }
    private val versionCounter: RAtomicLongReactive by lazy {
        api.redissonReactive.getAtomicLong(versionKey)
    }

    private val nearValues = buildNearCache<T>(nearValueCacheSize)
    private val nearIds = buildNearCache<Set<String>>(1)
    private val nearIndexIds = buildNearCache<Set<String>>(nearIndexCacheSize)

    private val refreshGate = Caffeine.newBuilder()
        .expireAfterWrite((ttl / 4).coerceIn(250.milliseconds, 1.hours))
        .maximumSize(100_000)
        .build<String, Unit>()


    private val lastVersion = AtomicLong(0L)
    private val cursorId = AtomicReference(StreamMessageId(0, 0))

    private val instanceId = api.clientId

    // Pre-computed per-instance constants reused on every Lua script invocation. These avoid
    // reallocating identical Strings / List<Any> wrappers on the hot path.
    private val messageDelimiterStr: String = MESSAGE_DELIMITER.toString()
    private val streamMaxLengthStr: String = STREAM_MAX_LENGTH.toString()
    private val ttlMillisStr: String = ttl.inWholeMilliseconds.toString()
    private val scriptKeys: List<Any> = listOf(idsRedisKey, streamKey, versionKey)
    private val touchScriptKeys: List<Any> = listOf(idsRedisKey)
    private val indicesSizeStr: String = indexes.all.size.toString()
    private val indexNames: Array<String> = indexes.all
        .map { index ->
            index.name.also { requireNoNul(it, "indexName") }
        }.toTypedArray()

    private fun <V : Any> buildNearCache(maxSize: Long) = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfter(object : Expiry<String, CacheEntry<V>> {
            private fun nanos(d: Duration): Long = d.inWholeNanoseconds.coerceAtLeast(1)
            override fun expireAfterCreate(k: String, v: CacheEntry<V>, t: Long) = when (v) {
                is CacheEntry.Value -> nanos(ttl)
                CacheEntry.Null -> nanos(ttl / 10)
            }

            override fun expireAfterUpdate(k: String, v: CacheEntry<V>, t: Long, d: Long) =
                expireAfterCreate(k, v, t)

            override fun expireAfterRead(k: String, v: CacheEntry<V>, t: Long, d: Long) = when (v) {
                is CacheEntry.Value -> nanos(ttl)
                CacheEntry.Null -> d // keep negative entry TTL on read
            }

        })
        .build<String, CacheEntry<V>>()


    override fun init(): Mono<Void> {
        if (isDisposed) return Mono.error(IllegalStateException("Cache '$namespace' is disposed"))

        return stream.fetchLatestStreamId()
            .doOnNext { id -> cursorId.set(id) }
            .doOnSuccess {
                trackDisposable(startPolling())
                trackDisposable(startRefreshingTtl())
            }
            .then()
    }

    override fun dispose0() {
        clearNearCacheOnly()
    }

    private fun startPolling() = stream.pollContinuously(cursorId) {
        onSuccess { batch ->
            for ((messageId, fields) in batch) {
                val type = fields[STREAM_FIELD_TYPE] ?: continue
                val msg = fields[STREAM_FIELD_MSG] ?: continue
                try {
                    processStreamMessage(type, msg)
                    cursorId.set(messageId)
                } catch (t: Throwable) {
                    log.atWarning().withCause(t)
                        .log("Error processing stream event in cache '$namespace' ($streamKey)")
                    handleStreamFault()
                }
            }
        }

        onFailure { e ->
            log.atWarning()
                .withCause(e)
                .log("Error polling stream for cache '$namespace' ($streamKey)")
            handleStreamFault()
        }
    }

    private fun startRefreshingTtl(): Disposable {
        return RedisExpirableUtils.refreshContinuously(ttl, stream, versionCounter)
    }

    private fun handleStreamFault() {
        lastVersion.set(0)
        clearNearCacheOnly()
    }

    private fun processStreamMessage(type: String, msg: String) {
        // Parse "version<NUL>origin<NUL>payload" without allocating an ArrayList per message.

        val firstDelim = msg.indexOf(MESSAGE_DELIMITER)
        if (firstDelim < 0) {
            log.atWarning().log("Malformed stream message on '$streamKey': $msg")
            return
        }
        val secondDelim = msg.indexOf(MESSAGE_DELIMITER, firstDelim + 1)
        val versionStr = msg.substring(0, firstDelim)
        val originId: String
        val payload: String
        if (secondDelim < 0) { // no payload
            originId = msg.substring(firstDelim + 1)
            payload = ""
        } else {
            originId = msg.substring(firstDelim + 1, secondDelim)
            payload = msg.substring(secondDelim + 1)
        }

        val version = versionStr.toLongOrNull()
        if (version == null) {
            log.atWarning().log("Invalid version '$versionStr' in stream message on '$streamKey'")
            return
        }

        val oldVersion = lastVersion.getAndUpdate { currentVer ->
            when {
                currentVer == 0L -> version
                version <= currentVer -> currentVer
                else -> version
            }
        }

        val shouldInvalidate = oldVersion != 0L && version > oldVersion + 1
        if (shouldInvalidate) {
            log.atWarning()
                .log("Version gap detected in cache '$namespace': last=$oldVersion, new=$version. Clearing near-cache.")
            clearNearCacheOnly()
            return
        }

        if (originId == instanceId) return

        when (type) {
            OP_ALL -> {
                clearNearCacheOnly()
            }

            OP_VAL -> {
                if (payload.isNotEmpty()) {
                    nearValues.invalidate(payload)
                    refreshGate.invalidate("$OP_VAL$MESSAGE_DELIMITER$payload")
                }
            }

            OP_IDS -> {
                nearIds.invalidate(LOCAL_IDS_CACHE_KEY)
                refreshGate.invalidate(OP_IDS)
            }

            OP_IDX -> {
                // Payload is "<indexName><NUL><indexValue>"
                val sepIdx = payload.indexOf(MESSAGE_DELIMITER)
                if (sepIdx >= 0) {
                    val idxName = payload.substring(0, sepIdx)
                    val idxValue = payload.substring(sepIdx + 1)
                    nearIndexIds.invalidate("$idxName$MESSAGE_DELIMITER$idxValue")
                    refreshGate.invalidate("$OP_IDX$MESSAGE_DELIMITER$idxName$MESSAGE_DELIMITER$idxValue")
                }
            }

            else -> {
                log.atWarning()
                    .log("Unknown cache invalidation type '$type' for cache '$namespace'")
            }
        }
    }

    private fun indexCacheKey(indexName: String, indexValue: String): String =
        "$indexName$MESSAGE_DELIMITER$indexValue"

    private fun refreshKeyVal(id: String): String = "$OP_VAL$MESSAGE_DELIMITER$id"
    private fun refreshKeyIds(): String = OP_IDS
    private fun refreshKeyIdx(indexName: String, indexValue: String): String =
        "$OP_IDX$MESSAGE_DELIMITER$indexName$MESSAGE_DELIMITER$indexValue"

    private fun refreshValueTtl(id: String) {
        refreshTtl(refreshKeyVal(id)) {
            val argv = arrayOfNulls<Any>(4 + indexNames.size)
            argv[0] = ttlMillisStr
            argv[1] = keyPrefix
            argv[2] = id
            argv[3] = indicesSizeStr
            for (i in indexNames.indices) {
                argv[4 + i] = indexNames[i]
            }

            @Suppress("UNCHECKED_CAST")
            scriptExecutor.execute<Long>(
                TOUCH_SCRIPT, RScript.Mode.READ_WRITE, RScript.ReturnType.LONG,
                keys = touchScriptKeys,
                *(argv as Array<Any>)
            ).subscribe(
                { /* no payload needed */ },
                { e ->
                    log.atWarning().withCause(e)
                        .log("Failed to refresh TTL via TOUCH_VALUE for id=$id")
                })
        }
    }

    private fun refreshIdsTtl() {
        refreshTtl(refreshKeyIds()) {
            api.redissonReactive.getSet<String>(idsRedisKey, StringCodec.INSTANCE)
                .expire(ttl.toJavaDuration())
                .doOnError { e ->
                    log.atWarning().withCause(e).log("Failed to refresh TTL for $idsRedisKey")
                }
                .subscribe()
        }
    }

    private fun refreshIndexTtl(indexName: String, indexValue: String) {
        val key = indexRedisKey(indexName, indexValue)
        refreshTtl(refreshKeyIdx(indexName, indexValue)) {
            api.redissonReactive.getSet<String>(key, StringCodec.INSTANCE)
                .expire(ttl.toJavaDuration())
                .doOnError { e ->
                    log.atWarning().withCause(e).log("Failed to refresh TTL for $key")
                }
                .subscribe()
        }
    }

    private fun refreshTtl(gateKey: String, action: () -> Unit) {
        val inserted = refreshGate.asMap().putIfAbsent(gateKey, Unit) == null
        if (!inserted) return
        action()
    }

    override suspend fun getCachedById(id: String): T? {
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

        val bucket =
            api.redissonReactive.getBucket<String>(valueRedisKey(normId), StringCodec.INSTANCE)
        val raw = bucket.get().awaitSingleOrNull() ?: run {
            nearValues.put(normId, CacheEntry.Null)
            return null
        }

        bucket.expire(ttl.toJavaDuration()).awaitSingleOrNull()
        refreshValueTtl(normId)

        val obj = if (raw == NULL_MARKER) null else api.json.decodeFromString(serializer, raw)

        if (obj == null) {
            nearValues.put(normId, CacheEntry.Null)
            return null
        }

        nearValues.put(normId, CacheEntry.Value(obj))

        return obj
    }

    override suspend fun <V : Any> findByIndexCached(index: RedisSetIndex<T, V>, value: V): Set<T> {
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
            else -> {
                val redisKey = indexRedisKey(index.name, queryValue)
                val set = api.redissonReactive.getSet<String>(redisKey, StringCodec.INSTANCE)
                val ids = set.readAll().awaitSingleOrNull().orEmpty()
                set.expire(ttl.toJavaDuration()).awaitSingleOrNull()

                nearIndexIds.put(
                    cacheKey,
                    if (ids.isEmpty()) CacheEntry.Null else CacheEntry.Value(ids)
                )
                ids
            }
        }

        if (loadedIds.isEmpty()) return emptySet()

        // Verify each ID's object (self-heal stale entries)
        val redisIndexKey = indexRedisKey(index.name, queryValue)
        val indexSet = api.redissonReactive.getSet<String>(redisIndexKey, StringCodec.INSTANCE)
        val result = ConcurrentHashMap.newKeySet<T>(loadedIds.size)
        val filteredIds = ConcurrentHashMap.newKeySet<String>(loadedIds.size)
        val changed = AtomicBoolean(false)
        val semaphore = Semaphore(MAX_CONCURRENT_REDIS_OPS)

        loadedIds.chunked(1000).forEach { chunk ->
            coroutineScope {
                for (id in chunk) {
                    launch {
                        semaphore.withPermit {
                            val element = getCachedById(id)
                            if (element == null) {
                                indexSet.remove(id).awaitSingleOrNull()
                                changed.set(true)
                            } else {
                                val actual = index.extractStrings(element)
                                if (queryValue !in actual) {
                                    indexSet.remove(id).awaitSingleOrNull()
                                    changed.set(true)
                                } else {
                                    filteredIds.add(id)
                                    result.add(element)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (changed.get()) {
            nearIndexIds.put(
                cacheKey,
                if (filteredIds.isEmpty()) CacheEntry.Null else CacheEntry.Value(filteredIds)
            )
        }

        return result
    }

    override suspend fun add(element: T): Boolean {
        val ttlMillis = ttl.inWholeMilliseconds
        require(ttlMillis > 0) { "ttl must be > 0ms" }

        val id = idOf(element).trim()
        require(id.isNotEmpty()) { "idOf(element) returned blank id" }
        requireNoNul(id, "id")

        val raw = api.json.encodeToString(serializer, element)

        val indices = indexes.all

        // Pre-extract all values so we know the total argv size upfront and can build the
        // Array<Any> directly without intermediate ArrayList allocations.
        var totalValues = 0
        val perIndexValues: Array<Set<String>> = Array(indices.size) { i ->
            val idx = indices[i]
            requireNoNul(idx.name, "indexName")
            val values = idx.extractStrings(element)
            for (v in values) requireNoNul(v, "indexValue")
            totalValues += values.size
            values
        }

        // Layout: instanceId, delim, maxLen, ttl, fieldType, fieldMsg, keyPrefix, id, raw,
        // indexCount, then for each index: name, valueCount, value1..valueN
        val argvSize = 10 + indices.size * 2 + totalValues
        val argv = arrayOfNulls<Any>(argvSize)
        argv[0] = instanceId
        argv[1] = messageDelimiterStr
        argv[2] = streamMaxLengthStr
        argv[3] = ttlMillisStr
        argv[4] = STREAM_FIELD_TYPE
        argv[5] = STREAM_FIELD_MSG
        argv[6] = keyPrefix
        argv[7] = id
        argv[8] = raw
        argv[9] = indicesSizeStr
        var pos = 10
        for (i in indices.indices) {
            argv[pos++] = indexNames[i]
            val values = perIndexValues[i]
            argv[pos++] = values.size.toString()
            for (v in values) {
                argv[pos++] = v
            }
        }

        @Suppress("UNCHECKED_CAST")
        val result = scriptExecutor.execute<List<Any>>(
            UPSERT_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LIST,
            scriptKeys,
            *(argv as Array<Any>),
        ).awaitSingle()

        val (wasNew, touchedIndices) = parseLuaFlagAndTouched(result)
        nearValues.put(id, CacheEntry.Value(element))

        if (wasNew) nearIds.invalidate(LOCAL_IDS_CACHE_KEY)
        for ((idxName, idxValue) in touchedIndices) {
            nearIndexIds.invalidate(indexCacheKey(idxName, idxValue))
        }

        return wasNew
    }

    override suspend fun findCached(condition: (T) -> Boolean): Set<T> {
        val ids = getAllIdsCached()
        if (ids.isEmpty()) return emptySet()

        val idsRedis = api.redissonReactive.getSet<String>(idsRedisKey, StringCodec.INSTANCE)
        val result = ConcurrentHashMap.newKeySet<T>()
        val semaphore = Semaphore(MAX_CONCURRENT_REDIS_OPS)

        ids.chunked(1000).forEach { chunk ->
            coroutineScope {
                for (id in chunk) {
                    launch {
                        semaphore.withPermit {
                            val value = getCachedById(id)
                            if (value == null) {
                                idsRedis.remove(id).awaitSingleOrNull() // stale
                                nearIds.invalidate(LOCAL_IDS_CACHE_KEY)
                            } else if (condition(value)) {
                                result.add(value)
                            }
                        }
                    }
                }
            }
        }


        return result
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

    override suspend fun findCachedOrLoadNullable(
        condition: (T) -> Boolean,
        loader: suspend () -> T?
    ): T? {
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
        val ids = getAllIdsCached()
        if (ids.isEmpty()) return false

        val removedAny = AtomicBoolean(false)
        val semaphore = Semaphore(MAX_CONCURRENT_REDIS_OPS)

        ids.chunked(1000).forEach { chunk ->
            coroutineScope {
                for (id in chunk) {
                    launch {
                        semaphore.withPermit {
                            val value = getCachedById(id)
                            if (value != null && predicate(value)) {
                                removedAny.set(true)
                                removeById(id)
                            }
                        }
                    }
                }
            }
        }

        return removedAny.get()
    }

    override suspend fun removeById(id: String): Boolean {
        val normId = id.trim()
        if (normId.isEmpty()) return false
        requireNoNul(normId, "id")

        val indices = indexes.all
        val argv = arrayOfNulls<Any>(9 + indices.size)
        argv[0] = instanceId
        argv[1] = messageDelimiterStr
        argv[2] = streamMaxLengthStr
        argv[3] = ttlMillisStr
        argv[4] = STREAM_FIELD_TYPE
        argv[5] = STREAM_FIELD_MSG
        argv[6] = keyPrefix
        argv[7] = normId
        argv[8] = indicesSizeStr
        for (i in indexNames.indices) {
            argv[9 + i] = indexNames[i]
        }

        @Suppress("UNCHECKED_CAST")
        val result = scriptExecutor.execute<List<Any>>(
            REMOVE_ID_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LIST,
            scriptKeys,
            *(argv as Array<Any>)
        ).awaitSingle()

        val (removed, touched) = parseLuaFlagAndTouched(result)

        if (!removed) return false

        nearValues.invalidate(normId)
        nearIds.invalidate(LOCAL_IDS_CACHE_KEY)
        refreshGate.invalidate(refreshKeyVal(normId))
        for ((idxName, idxValue) in touched) {
            nearIndexIds.invalidate(indexCacheKey(idxName, idxValue))
        }

        return true
    }

    override suspend fun <V : Any> removeByIndex(index: RedisSetIndex<T, V>, value: V): Boolean {
        require(indexes.containsSameInstance(index)) {
            "Index '${index.name}' is not registered in this cache instance. " +
                    "Use the index object from the same RedisSetIndexes registry that you passed into the cache."
        }

        val queryValue = index.valueString(value)
        requireNoNul(index.name, "indexName")
        requireNoNul(queryValue, "indexValue")

        val indices = indexes.all
        val argv = arrayOfNulls<Any>(10 + indices.size)
        argv[0] = instanceId
        argv[1] = messageDelimiterStr
        argv[2] = streamMaxLengthStr
        argv[3] = ttlMillisStr
        argv[4] = STREAM_FIELD_TYPE
        argv[5] = STREAM_FIELD_MSG
        argv[6] = keyPrefix
        argv[7] = index.name
        argv[8] = queryValue
        argv[9] = indicesSizeStr
        for (i in indexNames.indices) {
            argv[10 + i] = indexNames[i]
        }

        @Suppress("UNCHECKED_CAST")
        val removedCount = scriptExecutor.execute<Long>(
            REMOVE_INDEX_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            listOf(idsRedisKey, streamKey, versionKey),
            scriptKeys,
            *(argv as Array<Any>)
        ).awaitSingle()

        if (removedCount <= 0L) return false

        clearNearCacheOnly()
        return true
    }

    override suspend fun invalidateAll(): Long {
        val indices = indexes.all
        val argv = arrayOfNulls<Any>(8 + indices.size)
        argv[0] = instanceId
        argv[1] = messageDelimiterStr
        argv[2] = streamMaxLengthStr
        argv[3] = ttlMillisStr
        argv[4] = STREAM_FIELD_TYPE
        argv[5] = STREAM_FIELD_MSG
        argv[6] = keyPrefix
        argv[7] = indicesSizeStr
        for (i in indexNames.indices) {
            argv[8 + i] = indexNames[i]
        }

        @Suppress("UNCHECKED_CAST")
        val deletedCount = scriptExecutor.execute<Long>(
            CLEAR_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            scriptKeys,
            *(argv as Array<Any>)
        ).awaitSingle()

        clearNearCacheOnly()

        return deletedCount
    }

    private suspend fun getAllIdsCached(): Set<String> {
        when (val entry = nearIds.getIfPresent(LOCAL_IDS_CACHE_KEY)) {
            is CacheEntry.Value -> {
                refreshIdsTtl()
                return entry.value
            }

            CacheEntry.Null -> {
                return emptySet()
            }

            null -> {
                val set = api.redissonReactive.getSet<String>(idsRedisKey, StringCodec.INSTANCE)
                val ids = set.readAll().awaitSingleOrNull().orEmpty()
                set.expire(ttl.toJavaDuration()).awaitSingleOrNull()
                nearIds.put(
                    LOCAL_IDS_CACHE_KEY,
                    if (ids.isEmpty()) CacheEntry.Null else CacheEntry.Value(ids)
                )
                return ids
            }
        }
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