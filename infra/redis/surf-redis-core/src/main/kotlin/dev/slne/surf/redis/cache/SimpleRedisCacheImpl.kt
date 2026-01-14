package dev.slne.surf.redis.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterAccess
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.KSerializer
import org.redisson.RedissonSetCache
import org.redisson.api.options.KeysScanOptions
import reactor.core.Disposable
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.redisson.client.codec.StringCodec.INSTANCE as StringCodec

class SimpleRedisCacheImpl<K : Any, V : Any>(
    private val namespace: String,
    private val serializer: KSerializer<V>,
    private val keyToString: (K) -> String = { it.toString() },
    private val ttl: Duration,
    private val api: RedisApi
) : SimpleRedisCache<K, V> {
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
                                .log(
                                    "Received malformed cache invalidation message on topic $invalidationTopicName: ${
                                        message.toString().replace("{", "[").replace("}", "]")
                                    }"
                                )
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

    override fun dispose() {
        synchronized(this) {
            invalidationSubscriptionDisposable?.dispose()
            invalidationSubscriptionDisposable = null
            subscribed = false
        }
    }

    private fun redisKey(key: K): String = "$namespace:${keyToString(key)}"
    private fun localKey(key: K): String = keyToString(key)

    override suspend fun getCached(key: K): V? {
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

    override suspend fun put(key: K, value: V) {
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
        ensureSubscribed()
        val localKey = localKey(key)
        val redisKey = redisKey(key)

        api.redissonReactive.getBucket<String>(redisKey, StringCodec)
            .set(NULL_MARKER, ttl.toJavaDuration())
            .awaitSingleOrNull()

        nearCache.put(localKey, CacheEntry.Null)
        invalidationTopic.publish("$nodeId$MESSAGE_DELIMITER$localKey")
            .awaitSingle()
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

    override suspend fun invalidate(key: K): Long {
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

    override suspend fun invalidateAll(): Long {
        ensureSubscribed()

        val cache = api.redisson.getSetCache<String>("") as RedissonSetCache
        cache.iterator()
        cache.filter { true}

        val pattern = "$namespace:*"
        val keyOperations = api.redissonReactive.keys

        val keys = keyOperations
            .getKeys(KeysScanOptions.defaults().pattern(pattern))
            .collectList()
            .awaitSingle()

        val deleted = if (keys.isEmpty()) {
            0L
        } else {
            keyOperations
                .delete(*keys.toTypedArray())
                .awaitSingle()
        }

        nearCache.invalidateAll()
        refreshGate.invalidateAll()

        val prefix = "$namespace:"
        supervisorScope {
            for (redisKey in keys) {
                launch {
                    val localKey = redisKey.removePrefix(prefix)
                    invalidationTopic
                        .publish("$nodeId$MESSAGE_DELIMITER$localKey")
                        .awaitSingleOrNull()
                }
            }
        }

        return deleted
    }

    private sealed class CacheEntry<out V> {
        data class Value<V>(val value: V) : CacheEntry<V>()
        object Null : CacheEntry<Nothing>()
    }
}