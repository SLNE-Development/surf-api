package dev.slne.surf.redis.sync

import dev.slne.surf.redis.RedisTestBase
import dev.slne.surf.redis.sync.map.SyncMapChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncMapTest : RedisTestBase() {

    @Serializable
    data class TestKey(val id: String)

    @Serializable
    data class TestValue(val data: String)

    @Test
    fun `put should add new entry to map`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-put")

        val key = TestKey("key1")
        val value = TestValue("value1")

        val old = map.put(key, value)

        assertNull(old, "First put should return null")
        assertEquals(value, map.get(key), "Map should contain the new value")
        assertEquals(1, map.size(), "Map size should be 1")
    }

    @Test
    fun `put should replace existing entry`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-put-replace")

        val key = TestKey("key1")
        val value1 = TestValue("value1")
        val value2 = TestValue("value2")

        map.put(key, value1)
        val old = map.put(key, value2)

        assertEquals(value1, old, "Put should return the old value")
        assertEquals(value2, map.get(key), "Map should contain the new value")
        assertEquals(1, map.size(), "Map size should still be 1")
    }

    @Test
    fun `remove should remove existing entry`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-remove")

        val key = TestKey("key1")
        val value = TestValue("value1")

        map.put(key, value)
        val removed = map.remove(key)

        assertEquals(value, removed, "Remove should return the removed value")
        assertNull(map.get(key), "Map should not contain the key after removal")
        assertEquals(0, map.size(), "Map size should be 0")
    }

    @Test
    fun `remove should return null for non-existent key`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-remove-null")

        val key = TestKey("key1")

        val removed = map.remove(key)

        assertNull(removed, "Remove should return null for non-existent key")
    }

    @Test
    fun `clear should remove all entries`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-clear")

        val key1 = TestKey("key1")
        val key2 = TestKey("key2")
        val value1 = TestValue("value1")
        val value2 = TestValue("value2")

        map.put(key1, value1)
        map.put(key2, value2)

        assertEquals(2, map.size(), "Map should have 2 entries before clear")

        map.clear()

        assertEquals(0, map.size(), "Map should be empty after clear")
        assertNull(map.get(key1), "Map should not contain key1 after clear")
        assertNull(map.get(key2), "Map should not contain key2 after clear")
        assertTrue(map.isEmpty(), "Map should be empty")
    }

    @Test
    fun `clear on empty map should not notify listeners`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-clear-empty")

        var listenerCalled = false
        map.addListener { listenerCalled = true }

        map.clear()

        // Give async operations time to complete
        delay(100)

        assertEquals(false, listenerCalled, "Listener should not be called when clearing empty map")
    }

    @Test
    fun `put should notify listeners`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-put-listener")

        var change: SyncMapChange? = null
        map.addListener { change = it }

        val key = TestKey("key1")
        val value = TestValue("value1")

        map.put(key, value)

        assertTrue(change is SyncMapChange.Put<*, *>, "Listener should receive Put change")
        val putChange = change as SyncMapChange.Put<*, *>
        assertEquals(key, putChange.key, "Change should contain correct key")
        assertEquals(value, putChange.new, "Change should contain correct value")
        assertNull(putChange.old, "Change should have null old value")
    }

    @Test
    fun `remove should notify listeners`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-remove-listener")

        val key = TestKey("key1")
        val value = TestValue("value1")
        map.put(key, value)

        var change: SyncMapChange? = null
        map.addListener { change = it }

        map.remove(key)

        assertTrue(change is SyncMapChange.Removed<*, *>, "Listener should receive Removed change")
        val removedChange = change as SyncMapChange.Removed<*, *>
        assertEquals(key, removedChange.key, "Change should contain correct key")
        assertEquals(value, removedChange.removed, "Change should contain removed value")
    }

    @Test
    fun `clear should notify listeners`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-clear-listener")

        val key = TestKey("key1")
        val value = TestValue("value1")
        map.put(key, value)

        var change: SyncMapChange? = null
        map.addListener { change = it }

        map.clear()

        assertEquals(SyncMapChange.Cleared, change, "Listener should receive Cleared change")
    }

    @Test
    fun `operations should maintain consistency`() = runTest {
        val map = redisApi.createSyncMap<TestKey, TestValue>("test-map-consistency")

        // Add multiple entries
        val keys = (1..5).map { TestKey("key$it") }
        val values = (1..5).map { TestValue("value$it") }

        keys.zip(values).forEach { (key, value) ->
            map.put(key, value)
        }

        assertEquals(5, map.size())

        // Remove some entries
        map.remove(keys[1])
        map.remove(keys[3])

        assertEquals(3, map.size())
        assertNull(map.get(keys[1]))
        assertNull(map.get(keys[3]))

        // Update an entry
        val newValue = TestValue("updated")
        val old = map.put(keys[0], newValue)

        assertEquals(values[0], old)
        assertEquals(newValue, map.get(keys[0]))
        assertEquals(3, map.size())

        // Clear all
        map.clear()
        assertEquals(0, map.size())
        assertTrue(map.isEmpty())
    }
}
