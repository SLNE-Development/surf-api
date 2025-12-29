package dev.slne.surf.redis.sync

import dev.slne.surf.redis.RedisTestBase
import dev.slne.surf.redis.sync.list.SyncListChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncListTest : RedisTestBase() {

    @Test
    fun `removeIf removes matching elements`() = runTest {
        val syncList = redisApi.createSyncList("test-list-removeif-1", String.serializer())
        delay(100) // Allow initialization

        // Add elements
        syncList.add("apple")
        syncList.add("banana")
        syncList.add("cherry")
        syncList.add("apricot")
        delay(100) // Allow replication

        assertEquals(4, syncList.size())

        // Remove elements starting with 'a'
        val removed = syncList.removeIf { it.startsWith("a") }
        assertTrue(removed, "Should have removed elements")
        delay(100) // Allow replication

        assertEquals(2, syncList.size(), "Should have 2 elements left")
        assertEquals("banana", syncList.get(0))
        assertEquals("cherry", syncList.get(1))
    }

    @Test
    fun `removeIf returns false when no elements match`() = runTest {
        val syncList = redisApi.createSyncList("test-list-removeif-2", String.serializer())
        delay(100)

        syncList.add("apple")
        syncList.add("banana")
        delay(100)

        // Try to remove elements starting with 'z'
        val removed = syncList.removeIf { it.startsWith("z") }
        assertFalse(removed, "Should not have removed any elements")
        delay(100)

        assertEquals(2, syncList.size(), "All elements should still be present")
    }

    @Test
    fun `removeIf triggers listener for each removal`() = runTest {
        val syncList = redisApi.createSyncList("test-list-removeif-3", String.serializer())
        delay(100)

        val removedElements = mutableListOf<String>()
        syncList.addListener { change ->
            if (change is SyncListChange.Removed<*>) {
                removedElements.add(change.removed as String)
            }
        }

        syncList.add("apple")
        syncList.add("banana")
        syncList.add("cherry")
        delay(100)

        syncList.removeIf { it.startsWith("a") || it == "cherry" }
        delay(100)

        assertEquals(2, removedElements.size, "Should have notified for 2 removals")
        assertTrue(removedElements.contains("apple"))
        assertTrue(removedElements.contains("cherry"))
    }

    @Test
    fun `removeIf on empty list returns false`() = runTest {
        val syncList = redisApi.createSyncList("test-list-removeif-4", String.serializer())
        delay(100)

        val removed = syncList.removeIf { true }
        assertFalse(removed, "Should not have removed any elements from empty list")
    }

    @Test
    fun `removeIf preserves correct order`() = runTest {
        val syncList = redisApi.createSyncList("test-list-removeif-5", Int.serializer())
        delay(100)

        // Add numbers 1-10
        for (i in 1..10) {
            syncList.add(i)
        }
        delay(100)

        assertEquals(10, syncList.size())

        // Remove even numbers
        val removed = syncList.removeIf { it % 2 == 0 }
        assertTrue(removed, "Should have removed elements")
        delay(100)

        assertEquals(5, syncList.size(), "Should have 5 odd numbers left")
        assertEquals(1, syncList.get(0))
        assertEquals(3, syncList.get(1))
        assertEquals(5, syncList.get(2))
        assertEquals(7, syncList.get(3))
        assertEquals(9, syncList.get(4))
    }

    @Test
    fun `removeIf handles adjacent removals`() = runTest {
        val syncList = redisApi.createSyncList("test-list-removeif-6", String.serializer())
        delay(100)

        syncList.add("a")
        syncList.add("a")
        syncList.add("b")
        syncList.add("a")
        syncList.add("a")
        delay(100)

        assertEquals(5, syncList.size())

        // Remove all 'a' elements
        val removed = syncList.removeIf { it == "a" }
        assertTrue(removed, "Should have removed elements")
        delay(100)

        assertEquals(1, syncList.size(), "Should have 1 element left")
        assertEquals("b", syncList.get(0))
    }
}
