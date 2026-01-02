package dev.slne.surf.redis.cache

import dev.slne.surf.redis.RedisApi
import kotlinx.serialization.KSerializer
import java.io.Closeable

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
interface SimpleRedisCache<K : Any, V : Any> : Closeable {

    /**
     * Retrieve a value from the cache.
     *
     * Returns `null` if no entry exists or if a cached `null` marker is present.
     *
     * @param key The cache key.
     * @return The cached value of type [V], or `null` if absent or explicitly cached as `null`.
     */
    suspend fun getCached(key: K): V?

    /**
     * Store a value in the cache.
     *
     * The value is serialized to JSON using the provided [KSerializer] and stored with
     * the configured TTL.
     *
     * @param key The cache key.
     * @param value The value to store.
     */
    suspend fun put(key: K, value: V)

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
    suspend fun cachedOrLoad(key: K, loader: suspend () -> V): V

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
    suspend fun cachedOrLoadNullable(key: K, cacheNull: Boolean = false, loader: suspend () -> V?): V?

    /**
     * Remove an entry from the cache.
     *
     * @param key The cache key to remove.
     * @return The number of keys removed (typically 0 or 1).
     */
    suspend fun invalidate(key: K): Long
}