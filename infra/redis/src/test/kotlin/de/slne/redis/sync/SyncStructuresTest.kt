package de.slne.redis.sync

import de.slne.redis.sync.list.SyncList
import de.slne.redis.sync.map.SyncMap
import de.slne.redis.sync.set.SyncSet
import de.slne.redis.sync.value.SyncValue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncValueTest {
    
    @Test
    fun `test SyncValue default value`() {
        // This test verifies that SyncValue properly uses default values
        // Full integration testing requires a running Redis instance
        val defaultValue = 42
        
        try {
            runBlocking {
                val syncValue = SyncValue(
                    id = "test-value",
                    defaultValue = defaultValue,
                    redisUri = "redis://localhost:6379",
                    serializer = Int.serializer()
                )
                
                // Should return default value initially
                assertEquals(defaultValue, syncValue.get())
                
                syncValue.close()
            }
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(
                e.message?.contains("Unable to connect") == true ||
                e.message?.contains("Connection refused") == true ||
                e.cause?.message?.contains("Connection refused") == true
            )
        }
    }
    
    @Test
    fun `test SyncValue change listener subscription`() = runBlocking {
        // Test that listeners can be added and removed
        val defaultValue = "initial"
        
        try {
            val syncValue = SyncValue(
                id = "test-value-listener",
                defaultValue = defaultValue,
                redisUri = "redis://localhost:6379",
                serializer = String.serializer()
            )
            
            var changeNotified = false
            val listener = SyncChangeListener<String> { changeType, value, _ ->
                changeNotified = true
                assertEquals(SyncChangeType.SET, changeType)
                assertEquals("new-value", value)
            }
            
            syncValue.subscribe(listener)
            syncValue.unsubscribe(listener)
            syncValue.close()
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(
                e.message?.contains("Unable to connect") == true ||
                e.message?.contains("Connection refused") == true ||
                e.cause?.message?.contains("Connection refused") == true
            )
        }
    }
}

class SyncSetTest {
    
    @Test
    fun `test SyncSet initialization`() = runBlocking {
        // This test verifies that SyncSet can be initialized
        // Full integration testing requires a running Redis instance
        try {
            val syncSet = SyncSet(
                id = "test-set",
                redisUri = "redis://localhost:6379",
                serializer = String.serializer()
            )
            
            // Should be empty initially
            assertTrue(syncSet.isEmpty())
            assertEquals(0, syncSet.size())
            
            syncSet.close()
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(
                e.message?.contains("Unable to connect") == true ||
                e.message?.contains("Connection refused") == true ||
                e.cause?.message?.contains("Connection refused") == true
            )
        }
    }
}

class SyncMapTest {
    
    @Test
    fun `test SyncMap initialization`() = runBlocking {
        // This test verifies that SyncMap can be initialized
        // Full integration testing requires a running Redis instance
        try {
            val syncMap = SyncMap(
                id = "test-map",
                redisUri = "redis://localhost:6379",
                keySerializer = String.serializer(),
                valueSerializer = Int.serializer()
            )
            
            // Should be empty initially
            assertTrue(syncMap.isEmpty())
            assertEquals(0, syncMap.size())
            
            syncMap.close()
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(
                e.message?.contains("Unable to connect") == true ||
                e.message?.contains("Connection refused") == true ||
                e.cause?.message?.contains("Connection refused") == true
            )
        }
    }
}

class SyncListTest {
    
    @Test
    fun `test SyncList initialization`() = runBlocking {
        // This test verifies that SyncList can be initialized
        // Full integration testing requires a running Redis instance
        try {
            val syncList = SyncList(
                id = "test-list",
                redisUri = "redis://localhost:6379",
                serializer = String.serializer()
            )
            
            // Should be empty initially
            assertTrue(syncList.isEmpty())
            assertEquals(0, syncList.size())
            
            syncList.close()
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(
                e.message?.contains("Unable to connect") == true ||
                e.message?.contains("Connection refused") == true ||
                e.cause?.message?.contains("Connection refused") == true
            )
        }
    }
}

class SyncChangeTypeTest {
    
    @Test
    fun `test SyncChangeType enum values`() {
        // Verify all expected enum values exist
        assertEquals(3, SyncChangeType.values().size)
        assertTrue(SyncChangeType.values().contains(SyncChangeType.SET))
        assertTrue(SyncChangeType.values().contains(SyncChangeType.ADD))
        assertTrue(SyncChangeType.values().contains(SyncChangeType.REMOVE))
    }
}
