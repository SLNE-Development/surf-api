package dev.slne.surf.redis.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterAccess
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import reactor.core.Disposable
import java.io.Closeable
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.redisson.client.codec.StringCodec.INSTANCE as StringCodec

/**
 * A simple Redis-backed cache for values of type [V] parameterized by key type [K].
 *
 * Values are stored as JSON under keys formatted as `<namespace>:<key>`. A configurable
 * TTL (time-to-live) is applied to each entry. `null` values can optionally be cached
 * using a sentinel marker.
 *
 * This class uses the reactive Redis commands exposed by [RedisApi] and is intended
 * for coroutine-based usage (suspending methods).
 *
 * @param namespace String prefix that is prepended to each Redis key.
 * @param serializer [KSerializer] used to (de-)serialize values of type [V] to/from JSON.
 * @param keyToString Function that converts a key of type `K` to its String representation.
 *                    Default is `toString()`.
 * @param ttl Time-to-live for cache entries.
 * @param api Instance of [RedisApi] used to access Redis.
 */
class SimpleRedisCache<K : Any, V : Any> internal constructor(
    private val namespace: String,
    private val serializer: KSerializer<V>,
    private val keyToString: (K) -> String = { it.toString() },
    private val ttl: Duration,
    private val api: RedisApi
) : Closeable {
    private companion object {
        private val log = logger()
        private const val NULL_MARKER = "__NULL__"
        private const val INVALIDATION_TOPIC_SUFFIX = ":__cache_invalidate__"
        private const val MESSAGE_DELIMITER = '\u0000'

        private val nodeId = UUID.randomUUID().toString().replace("-", "")
    }

    private val invalidationTopicName = "$namespace$INVALIDATION_TOPIC_SUFFIX"
    private val invalidationTopic by lazy {
        api.redissonReactive.getTopic(invalidationTopicName, StringCodec)
    }
    private var invalidationSubscriptionDisposable: Disposable? = null

    @Volatile
    private var subscribed: Boolean = false

    private val nearCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(ttl)
        .build<String, CacheEntry<V>>()

    private val refreshGate = Caffeine.newBuilder()
        .expireAfterWrite(5.seconds)
        .maximumSize(100_000)
        .build<String, Unit>()

    private fun ensureSubscribed() {
        if (subscribed) return
        synchronized(this) {
            if (subscribed) return

            invalidationSubscriptionDisposable = invalidationTopic.getMessages(String::class.java)
                .subscribe(
                    { message ->
                        val parts = message.split(MESSAGE_DELIMITER, limit = 2)
                        if (parts.size == 2) {
                            val (publisherNodeId, keyString) = parts
                            // Only invalidate if the message was published by a different node
                            if (publisherNodeId != nodeId) {
                                nearCache.invalidate(keyString)
                                refreshGate.invalidate(keyString)
                            }
                        } else {
                            log.atWarning()
                                .log("Received malformed cache invalidation message on topic $invalidationTopicName: $message")
                        }
                    },
                    { error ->
                        log.atSevere()
                            .withCause(error)
                            .log("Error in cache invalidation subscription for topic $invalidationTopicName")
                    }
                )

            subscribed = true
        }
    }

    override fun close() {
        synchronized(this) {
            invalidationSubscriptionDisposable?.dispose()
            invalidationSubscriptionDisposable = null
            subscribed = false
        }
    }

    private fun redisKey(key: K): String = "$namespace:${keyToString(key)}"
    private fun localKey(key: K): String = keyToString(key)

    /**
     * Retrieve a value from the cache.
     *
     * Returns `null` if no entry exists or if a cached `null` marker is present.
     *
     * @param key The cache key.
     * @return The cached value of type [V], or `null` if absent or explicitly cached as `null`.
     */
    suspend fun getCached(key: K): V? {
        ensureSubscribed()

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
        val raw = bucket.get().awaitSingleOrNull() ?: return null
        bucket.expire(ttl.toJavaDuration()).awaitSingleOrNull()

        val entry = if (raw == NULL_MARKER) {
            CacheEntry.Null
        } else {
            CacheEntry.Value(api.json.decodeFromString(serializer, raw))
        }

        nearCache.put(localKey, entry)
        return (entry as? CacheEntry.Value)?.value
    }

    /**
     * Store a value in the cache.
     *
     * The value is serialized to JSON using the provided [KSerializer] and stored with
     * the configured TTL.
     *
     * @param key The cache key.
     * @param value The value to store.
     */
    suspend fun put(key: K, value: V) {
        ensureSubscribed()

        val localKey = localKey(key)
        val redisKey = redisKey(key)

        val raw = api.json.encodeToString(serializer, value)

        api.redissonReactive
            .getBucket<String>(redisKey, StringCodec)
            .set(raw, ttl.toJavaDuration())
            .awaitSingleOrNull()

        nearCache.put(localKey, CacheEntry.Value(value))
        invalidationTopic.publish("$nodeId$MESSAGE_DELIMITER$localKey").awaitSingle()
    }

    /**
     * Return the cached value or load it if absent.
     *
     * If no entry exists, the provided suspending `loader` is invoked, the result is cached
     * and then returned.
     *
     * @param key The cache key.
     * @param loader Suspended lambda to load the value if it is not present in the cache.
     * @return The existing or newly loaded value.
     */
    suspend fun cachedOrLoad(key: K, loader: suspend () -> V): V {
        getCached(key)?.let { return it }
        val loaded = loader()
        put(key, loaded)
        return loaded
    }

    /**
     * Return the cached value or load it if absent (nullable variant).
     *
     * If the loader returns `null`, the `cacheNull` flag controls whether a sentinel
     * marker is stored so subsequent calls do not trigger the loader again.
     *
     * @param key The cache key.
     * @param cacheNull If `true`, `null` results are cached using a sentinel marker.
     * @param loader Suspended lambda to load the nullable value if it is not present.
     * @return The existing or newly loaded value, or `null`.
     */
    suspend fun cachedOrLoadNullable(
        key: K,
        cacheNull: Boolean = false,
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
        ensureSubscribed()
        val localKey = localKey(key)
        val redisKey = redisKey(key)

        api.redissonReactive.getBucket<String>(redisKey, StringCodec)
            .set(NULL_MARKER, ttl.toJavaDuration())
            .awaitSingleOrNull()

        nearCache.put(localKey, CacheEntry.Null)
        invalidationTopic.publish("$nodeId$MESSAGE_DELIMITER$localKey").awaitSingle()
    }

    private fun refreshTtl(localKey: String, redisKey: String) {
        val inserted = refreshGate.asMap().putIfAbsent(localKey, Unit) == null
        if (!inserted) return

        api.redissonReactive
            .getBucket<String>(redisKey, StringCodec)
            .expire(ttl.toJavaDuration())
            .doOnError { e ->
                log.atWarning()
                    .withCause(e)
                    .log("Failed to refresh TTL for $redisKey")
            }
            .subscribe()
    }

    /**
     * Remove an entry from the cache.
     *
     * @param key The cache key to remove.
     * @return The number of keys removed (typically 0 or 1).
     */
    suspend fun invalidate(key: K): Long {
        ensureSubscribed()

        val localKey = localKey(key)
        val redisKey = redisKey(key)

        val deleted = api.redissonReactive
            .getBucket<String>(redisKey, StringCodec)
            .delete()
            .awaitSingle()

        nearCache.invalidate(localKey)
        refreshGate.invalidate(localKey)
        invalidationTopic.publish("$nodeId$MESSAGE_DELIMITER$localKey").awaitSingle()

        return if (deleted) 1L else 0L
    }

    private sealed class CacheEntry<out V> {
        data class Value<V>(val value: V) : CacheEntry<V>()
        object Null : CacheEntry<Nothing>()
    }
}