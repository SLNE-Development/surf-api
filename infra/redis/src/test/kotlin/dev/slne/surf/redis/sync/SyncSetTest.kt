package dev.slne.surf.redis.sync

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.RedisTestBase
import dev.slne.surf.redis.sync.set.SyncSetChange
import io.lettuce.core.RedisURI
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

    @Test
    fun `removeIf replicates correctly across multiple nodes`() = runTest {
        // Create two nodes connected to the same Redis instance
        val node1Api = RedisApi.create(RedisURI.create(redisContainer.redisURI))
        node1Api.freezeAndConnect()
        
        val node2Api = RedisApi.create(RedisURI.create(redisContainer.redisURI))
        node2Api.freezeAndConnect()

        try {
            val set1 = node1Api.createSyncSet("test-set-multinode-1", String.serializer())
            val set2 = node2Api.createSyncSet("test-set-multinode-1", String.serializer())
            delay(200) // Allow both to initialize and sync

            // Add elements from node1
            set1.add("apple")
            set1.add("banana")
            set1.add("cherry")
            set1.add("apricot")
            set1.add("date")
            delay(200) // Allow replication

            // Verify both nodes see the same data
            assertEquals(5, set1.size(), "Node 1 should have 5 elements")
            assertEquals(5, set2.size(), "Node 2 should have 5 elements")

            // Remove elements from node1
            val removed = set1.removeIf { it.startsWith("a") }
            assertTrue(removed, "Should have removed elements")
            delay(300) // Allow replication of Remove deltas

            // Verify both nodes have the same final state
            assertEquals(3, set1.size(), "Node 1 should have 3 elements left")
            assertEquals(3, set2.size(), "Node 2 should have 3 elements left")
            
            // Check the same elements are present on both nodes
            assertTrue(set1.contains("banana"))
            assertTrue(set1.contains("cherry"))
            assertTrue(set1.contains("date"))
            assertFalse(set1.contains("apple"))
            assertFalse(set1.contains("apricot"))
            
            assertTrue(set2.contains("banana"))
            assertTrue(set2.contains("cherry"))
            assertTrue(set2.contains("date"))
            assertFalse(set2.contains("apple"))
            assertFalse(set2.contains("apricot"))
        } finally {
            node1Api.disconnect()
            node2Api.disconnect()
        }
    }
}
