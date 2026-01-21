package dev.slne.surf.redis.cache

import dev.slne.surf.redis.util.Initializable
import reactor.core.Disposable

/**
 * A Redis-backed cache for a *set of entities* with:
 * - a primary id (idOf) to avoid duplicates (upserts instead of "append JSON to a set")
 * - optional secondary indexes (e.g. memberId -> clanIds)
 * - a local near-cache (Caffeine)
 *
 * Note:
 * - `condition: (T) -> Boolean` can NOT be evaluated server-side, so `findCached(condition)` always scans.
 * - Index queries are fast (use Redis sets), but still validate results to self-heal stale index entries.
 */
interface SimpleSetRedisCache<T : Any> : Disposable, Initializable {

    suspend fun findCached(condition: (T) -> Boolean): Set<T>
    suspend fun getCachedById(id: String): T?
    suspend fun <V : Any> findByIndexCached(index: RedisSetIndex<T, V>, value: V): Set<T>
    suspend fun <V : Any> findFirstByIndexCached(index: RedisSetIndex<T, V>, value: V): T? =
        findByIndexCached(index, value).firstOrNull()

    suspend fun add(element: T): Boolean

    suspend fun findCachedOrLoad(condition: (T) -> Boolean, loader: suspend () -> T): T
    suspend fun <V : Any> findCachedByIndexOrLoad(index: RedisSetIndex<T, V>, value: V, loader: suspend () -> T): T

    suspend fun findCachedOrLoadNullable(condition: (T) -> Boolean, loader: suspend () -> T?): T?
    suspend fun <V : Any> findCachedByIndexOrLoadNullable(
        index: RedisSetIndex<T, V>,
        value: V,
        loader: suspend () -> T?
    ): T?

    suspend fun removeIf(predicate: (T) -> Boolean): Boolean

    suspend fun removeById(id: String): Boolean
    suspend fun <V : Any> removeByIndex(index: RedisSetIndex<T, V>, value: V): Boolean

    suspend fun invalidateAll(): Long
    fun clearNearCacheOnly()
}