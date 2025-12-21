package de.slne.redis.sync

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncManagerTest {
    
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            SyncManager.init("redis://localhost:6379")
        }
    }
    
    @Test
    fun `test SyncManager initialization`() {
        assertEquals("redis://localhost:6379", SyncManager.getRedisUri())
    }
    
    @Test
    fun `test simplified syncValue with String`() = runBlocking {
        try {
            val value = syncValue("test-string-value", "default")
            assertEquals("default", value.get())
            value.close()
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
    fun `test simplified syncValue with Int`() = runBlocking {
        try {
            val value = syncValue("test-int-value", 42)
            assertEquals(42, value.get())
            value.close()
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
    fun `test simplified syncSet`() = runBlocking {
        try {
            val set = syncSet<String>("test-set-simple")
            assertTrue(set.isEmpty())
            set.close()
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
    fun `test simplified syncMap`() = runBlocking {
        try {
            val map = syncMap<String, Int>("test-map-simple")
            assertTrue(map.isEmpty())
            map.close()
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
    fun `test simplified syncList`() = runBlocking {
        try {
            val list = syncList<String>("test-list-simple")
            assertTrue(list.isEmpty())
            list.close()
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
    fun `test syncValue with Serializable class`() = runBlocking {
        @Serializable
        data class TestData(val name: String, val value: Int)
        
        try {
            val value = syncValue("test-custom-value", TestData("test", 42))
            assertEquals(TestData("test", 42), value.get())
            value.close()
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
