package dev.slne.surf.redis.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterAccess
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.*
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import org.redisson.RedissonObject
import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RScript
import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.redisson.client.codec.StringCodec.INSTANCE as StringCodec

class SimpleRedisCacheImpl<K : Any, V : Any>(
    n: String,
    private val serializer: KSerializer<V>,
    private val keyToString: (K) -> String = { it.toString() },
    private val ttl: Duration,
    private val api: RedisApi
) : SimpleRedisCache<K, V>, DisposableAware() {
    private companion object {
        private val log = logger()

        const val STREAM_FIELD_TYPE = "T"
        const val STREAM_FIELD_MSG = "M"

        private const val STREAM_SUFFIX = ":__stream__"
        private const val VERSION_KEY_SUFFIX = ":__version__"
        private const val IDS_KEY_SUFFIX = ":__ids__"
        private const val VALUE_KEY_INFIX = ":__val__:"
        private const val NULL_MARKER = "__NULL__"
        private const val MESSAGE_DELIMITER = '\u0000'
        private const val MAX_STREAM_LENGTH = 10_000
        private const val OP_ALL = "A"
        private const val OP_VAL = "V"

        private const val PUT_SCRIPT = "put"
        private const val PUT_NULL_SCRIPT = "put_null"
        private const val REMOVE_SCRIPT = "remove_by_id"
        private const val CLEAR_SCRIPT = "clear"
        private const val TOUCH_SCRIPT = "touch_value"

        private object LuaScripts : LuaScriptRegistry("lua/cache/simple") {
            init {
                load(PUT_SCRIPT)
                load(PUT_NULL_SCRIPT)
                load(REMOVE_SCRIPT)
                load(CLEAR_SCRIPT)
                load(TOUCH_SCRIPT)
            }
        }

        private fun requireNoNul(s: String, label: String) {
            require(!s.contains(MESSAGE_DELIMITER)) { "$label must not contain NUL character" }
        }
    }

    init {
        require(n.isNotBlank()) { "Namespace must not be blank" }
        requireNoNul(n, "Namespace")
        require(!n.contains('{') && !n.contains('}')) { "Namespace must not contain '{' or '}'" }
    }

    private val namespace = n.replace(":", "_")
    private val slotTag = "{$namespace}"
    private val keyPrefix = "$namespace:$slotTag"

    private val streamKey = "$keyPrefix$STREAM_SUFFIX"
    private val versionKey = "$keyPrefix$VERSION_KEY_SUFFIX"
    private val idsKey = "$keyPrefix$IDS_KEY_SUFFIX"

    private val instanceId = api.clientId

    private val scriptExecutor = LuaScriptExecutor.getInstance(api, LuaScripts)
    private val stream: RStreamReactive<String, String> by lazy {
        api.redissonReactive.getStream<String, String>(streamKey, StringCodec)
    }
    private val versionCounter: RAtomicLongReactive by lazy {
        api.redissonReactive.getAtomicLong(versionKey)
    }

    private val nearCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(ttl)
        .build<String, CacheEntry<V>>()
    private val refreshGate = Caffeine.newBuilder()
        .expireAfterWrite(5.seconds)
        .maximumSize(100_000)
        .build<String, Unit>()


    private val lastVersion = AtomicLong(0L)
    private val cursorId = AtomicReference<StreamMessageId>(StreamMessageId(0, 0))

    private fun redisKey(key: K): String = "$keyPrefix$VALUE_KEY_INFIX${keyToString(key)}"
    private fun localKey(key: K): String = keyToString(key)

    override fun init(): Mono<Void> {
        if (isDisposed) return Mono.error(IllegalStateException("Cache '$namespace' is disposed"))

        return stream.fetchLatestStreamId()
            .doOnNext { cursorId.set(it) }
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
        // version<NUL>origin<NUL>payload
        val parts = msg.split(MESSAGE_DELIMITER, limit = 3)
        if (parts.size < 2) {
            log.atWarning().log("Malformed stream message in cache '$namespace': $msg")
            return
        }
        val versionStr = parts[0]
        val originId = parts[1]
        val payload = if (parts.size >= 3) parts[2] else ""

        val version = versionStr.toLongOrNull()
        if (version == null) {
            log.atWarning().log("Invalid version '$versionStr' in stream message for cache '$namespace'")
            return
        }

        val shouldInvalidate = AtomicBoolean(false)
        val oldVersion = lastVersion.getAndUpdate { currentVer ->
            when {
                currentVer == 0L -> version
                version <= currentVer -> currentVer
                version == currentVer + 1 -> version
                else -> {
                    shouldInvalidate.set(true)
                    version
                }
            }
        }

        if (shouldInvalidate.get()) {
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
                    nearCache.invalidate(payload)
                    refreshGate.invalidate(payload)
                }
            }

            else -> {
                log.atWarning().log("Unknown event type '$type' in stream for cache '$namespace'")
            }
        }
    }

    override suspend fun getCached(key: K): V? {
        val localKey = localKey(key)
        val redisKey = redisKey(key)

        when (val entry = nearCache.getIfPresent(localKey)) {
            is CacheEntry.Value -> {
                refreshTtl(localKey, redisKey)
                return entry.value
            }

            CacheEntry.Null -> {
                refreshTtl(localKey, redisKey)
                return null
            }

            null -> Unit // miss
        }

        val bucket = api.redissonReactive.getBucket<String>(redisKey, StringCodec)
        val raw = bucket.get().awaitSingleOrNull()

        if (raw == null) {
            nearCache.put(localKey, CacheEntry.Null)
            return null
        }

        bucket.expire(ttl.toJavaDuration()).awaitSingleOrNull()

        val entry = if (raw == NULL_MARKER) {
            CacheEntry.Null
        } else {
            CacheEntry.Value(api.json.decodeFromString(serializer, raw))
        }

        nearCache.put(localKey, entry)
        return (entry as? CacheEntry.Value)?.value
    }

    override suspend fun put(key: K, value: V) {
        val localKey = localKey(key)
        requireNoNul(localKey, "key")
        val raw = api.json.encodeToString(serializer, value)

        val argv = ObjectArrayList<String>(10)
        argv += instanceId
        argv += MESSAGE_DELIMITER.toString()
        argv += MAX_STREAM_LENGTH.toString()
        argv += ttl.inWholeMilliseconds.toString()
        argv += STREAM_FIELD_TYPE
        argv += STREAM_FIELD_MSG
        argv += OP_VAL
        argv += localKey
        argv += raw
        argv += keyPrefix

        scriptExecutor.execute<Long>(
            PUT_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            listOf(idsKey, streamKey, versionKey),
            *argv.toTypedArray()
        ).awaitSingle()

        nearCache.put(localKey, CacheEntry.Value(value))
    }

    override suspend fun cachedOrLoad(key: K, loader: suspend () -> V): V {
        getCached(key)?.let { return it }
        val loaded = loader()
        put(key, loaded)
        return loaded
    }

    override suspend fun cachedOrLoadNullable(
        key: K,
        cacheNull: Boolean,
        loader: suspend () -> V?
    ): V? {
        val existing = getCached(key)
        if (existing != null) return existing
        if (nearCache.getIfPresent(localKey(key)) == CacheEntry.Null) return null

        val loaded = loader()
        when {
            loaded != null -> put(key, loaded)
            cacheNull -> putNull(key)
        }
        return loaded
    }

    private suspend fun putNull(key: K) {
        val localKey = localKey(key)
        requireNoNul(localKey, "key")

        val argv = ObjectArrayList<String>(9)
        argv += instanceId
        argv += MESSAGE_DELIMITER.toString()
        argv += MAX_STREAM_LENGTH.toString()
        argv += ttl.inWholeMilliseconds.toString()
        argv += STREAM_FIELD_TYPE
        argv += STREAM_FIELD_MSG
        argv += OP_VAL
        argv += localKey
        argv += keyPrefix

        scriptExecutor.execute<Long>(
            PUT_NULL_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            listOf(idsKey, streamKey, versionKey),
            *argv.toTypedArray()
        ).awaitSingle()
        nearCache.put(localKey, CacheEntry.Null)
    }

    override suspend fun invalidate(key: K): Long {
        val localKey = localKey(key)
        requireNoNul(localKey, "key")

        val argv = ObjectArrayList<String>(9)
        argv += instanceId
        argv += MESSAGE_DELIMITER.toString()
        argv += MAX_STREAM_LENGTH.toString()
        argv += ttl.inWholeMilliseconds.toString()
        argv += STREAM_FIELD_TYPE
        argv += STREAM_FIELD_MSG
        argv += OP_VAL
        argv += localKey
        argv += keyPrefix

        val removed: Long = scriptExecutor.execute<Long>(
            REMOVE_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            listOf(idsKey, streamKey, versionKey),
            *argv.toTypedArray()
        ).awaitSingle()

        if (removed > 0) {
            nearCache.invalidate(localKey)
            refreshGate.invalidate(localKey)
        }

        return removed
    }

    override suspend fun invalidateAll(): Long {
        val argv = ObjectArrayList<String>(8)
        argv += instanceId
        argv += MESSAGE_DELIMITER.toString()
        argv += MAX_STREAM_LENGTH.toString()
        argv += ttl.inWholeMilliseconds.toString()
        argv += STREAM_FIELD_TYPE
        argv += STREAM_FIELD_MSG
        argv += OP_ALL
        argv += keyPrefix

        val deleted: Long = scriptExecutor.execute<Long>(
            CLEAR_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            listOf(idsKey, streamKey, versionKey),
            *argv.toTypedArray()
        ).awaitSingle()
        clearNearCacheOnly()

        return deleted
    }

    private fun refreshTtl(localKey: String, redisKey: String) {
        val shouldRefresh = refreshGate.asMap().putIfAbsent(localKey, Unit) == null
        if (!shouldRefresh) return

        val argv = ObjectArrayList<String>(3)
        argv += ttl.inWholeMilliseconds.toString()
        argv += localKey
        argv += keyPrefix

        scriptExecutor.execute<Long>(
            TOUCH_SCRIPT,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            keys = listOf(idsKey),
            *argv.toTypedArray()
        ).subscribe(
            { /* result isn't used */ },
            { e ->
                log.atWarning()
                    .withCause(e)
                    .log("Failed to refresh TTL for key $redisKey in cache '$namespace'")
            }
        )
    }

    private fun clearNearCacheOnly() {
        nearCache.invalidateAll()
        refreshGate.invalidateAll()
    }

    private sealed class CacheEntry<out V> {
        data class Value<V>(val value: V) : CacheEntry<V>()
        object Null : CacheEntry<Nothing>()
    }
}