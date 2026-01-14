package dev.slne.surf.redis.cache

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.RedisTestBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class SimpleRedisCacheTest : RedisTestBase() {

    @Serializable
    data class TestValue(val data: String)

    var cache: SimpleRedisCache<String, TestValue> by Delegates.notNull()

    override fun beforeApiFreeze(api: RedisApi) {
        cache = api.createSimpleCache(
            namespace = "test-cache",
            ttl = 60.seconds
        )
    }

    @Test
    fun `test self-invalidation is prevented`() = runBlocking {
        val key = "test-key"
        val value = TestValue("test-data")

        // Put a value - this should update local cache and publish invalidation
        cache.put(key, value)

        // Wait a bit for any invalidation messages to be processed
        delay(200)

        // The local cache should still contain the value (not self-invalidated)
        val cachedValue = cache.getCached(key)
        assertEquals(value, cachedValue, "Value should still be in local cache after put")

        cache.close()
    }

    @Test
    fun `test put and get cached value`() = runBlocking {
        val key = "key1"
        val value = TestValue("value1")

        // Put and retrieve
        cache.put(key, value)
        val retrieved = cache.getCached(key)

        assertEquals(value, retrieved, "Retrieved value should match the put value")

        cache.close()
    }

    @Test
    fun `test invalidate removes value`() = runBlocking {
        val key = "key2"
        val value = TestValue("value2")

        // Put, then invalidate
        cache.put(key, value)
        cache.invalidate(key)

        // Wait a bit for invalidation
        delay(100)

        // Value should be gone from both Redis and local cache
        val retrieved = cache.getCached(key)
        assertNull(retrieved, "Value should be null after invalidation")

        cache.close()
    }

    @Test
    fun `test cachedOrLoad with loader`() = runBlocking {
        val key = "key3"
        val value = TestValue("loaded-value")

        var loaderCallCount = 0

        // First call should invoke loader
        val result1 = cache.cachedOrLoad(key) {
            loaderCallCount++
            value
        }

        assertEquals(value, result1, "First call should return loaded value")
        assertEquals(1, loaderCallCount, "Loader should be called once")

        // Second call should use cached value
        val result2 = cache.cachedOrLoad(key) {
            loaderCallCount++
            TestValue("should-not-be-called")
        }

        assertEquals(value, result2, "Second call should return cached value")
        assertEquals(1, loaderCallCount, "Loader should not be called again")

        cache.close()
    }

    @Test
    fun `test putNull caches null sentinel`() = runBlocking {
        val key = "null-key"

        // Use cachedOrLoadNullable with cacheNull=true to store null
        val result = cache.cachedOrLoadNullable(key, cacheNull = true) {
            null
        }

        assertNull(result, "Result should be null")

        // Second call should return cached null without invoking loader
        var loaderCalled = false
        val result2 = cache.cachedOrLoadNullable(key, cacheNull = true) {
            loaderCalled = true
            TestValue("should-not-be-called")
        }

        assertNull(result2, "Cached null should be returned")
        assertEquals(false, loaderCalled, "Loader should not be called for cached null")

        cache.close()
    }
}