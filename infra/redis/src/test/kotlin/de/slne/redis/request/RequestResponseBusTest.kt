package de.slne.redis.request

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedisRequestTest {
    
    @Serializable
    data class TestRequest(val message: String, val value: Int) : RedisRequest()
    
    @Test
    fun `test request creation includes timestamp`() {
        val request = TestRequest("test", 42)
        assertTrue(request.timestamp > 0)
        assertTrue(request.timestamp <= System.currentTimeMillis())
    }
    
    @Test
    fun `test request properties are accessible`() {
        val request = TestRequest("hello", 123)
        assertEquals("hello", request.message)
        assertEquals(123, request.value)
    }
}

class RedisResponseTest {
    
    @Serializable
    data class TestResponse(val data: String) : RedisResponse()
    
    @Test
    fun `test response creation includes timestamp`() {
        val response = TestResponse("test data")
        assertTrue(response.timestamp > 0)
        assertTrue(response.timestamp <= System.currentTimeMillis())
    }
    
    @Test
    fun `test response properties are accessible`() {
        val response = TestResponse("hello world")
        assertEquals("hello world", response.data)
    }
}

class RequestHandlerAnnotationTest {
    
    @Test
    fun `test RequestHandler annotation can be applied to methods`() {
        val methods = TestHandler::class.java.declaredMethods
        val method = methods.find { 
            it.name == "handleRequest" && it.isAnnotationPresent(RequestHandler::class.java)
        }
        assertTrue(method != null, "RequestHandler method should be found")
        assertTrue(method.isAnnotationPresent(RequestHandler::class.java))
    }
    
    @Serializable
    data class TestRequest(val data: String) : RedisRequest()
    
    @Serializable
    data class TestResponse(val result: String) : RedisResponse()
    
    class TestHandler {
        @RequestHandler
        fun handleRequest(context: RequestContext<TestRequest>) {
            context.coroutineScope.launch {
                context.respond(TestResponse("processed: ${context.request.data}"))
            }
        }
    }
}

class RequestResponseBusTest {
    
    @Serializable
    data class TestRequest(val message: String) : RedisRequest()
    
    @Serializable
    data class TestResponse(val result: String) : RedisResponse()
    
    @Test
    fun `test handler registration scans RequestHandler methods`() = runBlocking {
        // This test verifies that handler registration doesn't throw exceptions
        // Full integration testing requires a running Redis instance
        val handler = TestHandler()
        
        try {
            val bus = RequestResponseBus("redis://localhost:6379")
            bus.registerRequestHandler(handler)
            bus.unregisterRequestHandler(handler)
            bus.close()
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(
                e.message?.contains("Unable to connect") == true || 
                e.message?.contains("Connection refused") == true ||
                e.cause?.message?.contains("Connection refused") == true
            )
        }
    }
    
    class TestHandler {
        @RequestHandler
        fun handleTestRequest(context: RequestContext<TestRequest>) {
            context.coroutineScope.launch {
                context.respond(TestResponse("handled: ${context.request.message}"))
            }
        }
    }
}
