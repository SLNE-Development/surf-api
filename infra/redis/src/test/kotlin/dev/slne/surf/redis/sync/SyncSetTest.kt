package dev.slne.surf.redis.sync

import dev.slne.surf.redis.RedisTestBase
import dev.slne.surf.redis.sync.set.SyncSetChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncSetTest : RedisTestBase() {

    @Test
    fun `removeIf removes matching elements`() = runTest {
        val syncSet = redisApi.createSyncSet("test-set-removeif-1", String.serializer())
        delay(100) // Allow initialization

        // Add elements
        syncSet.add("apple")
        syncSet.add("banana")
        syncSet.add("cherry")
        syncSet.add("apricot")
        delay(100) // Allow replication

        assertEquals(4, syncSet.size())

        // Remove elements starting with 'a'
        val removed = syncSet.removeIf { it.startsWith("a") }
        assertTrue(removed, "Should have removed elements")
        delay(100) // Allow replication

        assertEquals(2, syncSet.size(), "Should have 2 elements left")
        assertFalse(syncSet.contains("apple"))
        assertFalse(syncSet.contains("apricot"))
        assertTrue(syncSet.contains("banana"))
        assertTrue(syncSet.contains("cherry"))
    }

    @Test
    fun `removeIf returns false when no elements match`() = runTest {
        val syncSet = redisApi.createSyncSet("test-set-removeif-2", String.serializer())
        delay(100)

        syncSet.add("apple")
        syncSet.add("banana")
        delay(100)

        // Try to remove elements starting with 'z'
        val removed = syncSet.removeIf { it.startsWith("z") }
        assertFalse(removed, "Should not have removed any elements")
        delay(100)

        assertEquals(2, syncSet.size(), "All elements should still be present")
    }

    @Test
    fun `removeIf triggers listener for each removal`() = runTest {
        val syncSet = redisApi.createSyncSet("test-set-removeif-3", String.serializer())
        delay(100)

        val removedElements = mutableListOf<String>()
        syncSet.addListener { change ->
            if (change is SyncSetChange.Removed<*>) {
                removedElements.add(change.element as String)
            }
        }

        syncSet.add("apple")
        syncSet.add("banana")
        syncSet.add("cherry")
        delay(100)

        syncSet.removeIf { it.startsWith("a") || it == "cherry" }
        delay(100)

        assertEquals(2, removedElements.size, "Should have notified for 2 removals")
        assertTrue(removedElements.contains("apple"))
        assertTrue(removedElements.contains("cherry"))
    }

    @Test
    fun `removeIf on empty set returns false`() = runTest {
        val syncSet = redisApi.createSyncSet("test-set-removeif-4", String.serializer())
        delay(100)

        val removed = syncSet.removeIf { true }
        assertFalse(removed, "Should not have removed any elements from empty set")
    }
}
