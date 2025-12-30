package dev.slne.surf.redis.cache

import dev.slne.surf.redis.RedisApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.serialization.KSerializer
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.client.codec.StringCodec
import kotlin.time.Duration
import kotlin.time.toJavaDuration

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
 * @param name String prefix that is prepended to each Redis key.
 * @param serializer [KSerializer] used to (de-)serialize values of type [V] to/from JSON.
 * @param keyToString Function that converts a key of type `K` to its String representation.
 *                    Default is `toString()`.
 * @param ttl Time-to-live for cache entries.
 * @param api Instance of [RedisApi] used to access Redis.
 */
class SimpleRedisCache<K : Any, V : Any> internal constructor(
    private val name: String,
    private val serializer: KSerializer<V>,
    private val keyToString: (K) -> String = { it.toString() },
    private val ttl: Duration,
    private val api: RedisApi
) {
    private companion object {
        private const val NULL_MARKER = "__NULL__"
    }

    private val cache by lazy {
        api.redissonReactive
        api.redissonReactive.getLocalCachedMap(
            LocalCachedMapOptions.name<String, String>(name)
                .codec(StringCodec.INSTANCE)
                .cacheSize(10_000)
                .maxIdle(ttl.toJavaDuration())
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE)
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
                .cacheProvider(LocalCachedMapOptions.CacheProvider.CAFFEINE)
        )
    }

    /**
     * Retrieve a value from the cache.
     *
     * Returns `null` if no entry exists or if a cached `null` marker is present.
     *
     * @param key The cache key.
     * @return The cached value of type [V], or `null` if absent or explicitly cached as `null`.
     */
    suspend fun getCached(key: K): V? {
        val raw = cache.get(keyToString(key)).awaitSingleOrNull() ?: return null

        if (raw == NULL_MARKER) {
            return null
        }

        return api.json.decodeFromString(serializer, raw)
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
        putRaw(key, api.json.encodeToString(serializer, value))
    }

    /**
     * Internal helper to store already serialized data or sentinel markers.
     *
     * @param key The cache key.
     * @param raw The raw string to store (JSON or sentinel marker).
     */
    private suspend fun putRaw(key: K, raw: String) {
        cache.fastPut(keyToString(key), raw).awaitSingle()
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
        getCached(key)?.let { return it }

        val loaded = loader()
        when {
            loaded != null -> put(key, loaded)
            cacheNull -> putRaw(key, NULL_MARKER)
        }

        return loaded
    }

    /**
     * Remove an entry from the cache.
     *
     * @param key The cache key to remove.
     * @return The number of keys removed (typically 0 or 1).
     */
    suspend fun invalidate(key: K): Long {
        return cache.fastRemove(keyToString(key)).awaitSingle()
    }
}