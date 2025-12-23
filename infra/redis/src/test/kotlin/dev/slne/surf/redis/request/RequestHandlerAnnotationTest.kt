package dev.slne.surf.redis.request

import dev.slne.surf.redis.request.HandleRedisRequest
import dev.slne.surf.redis.request.RedisRequest
import dev.slne.surf.redis.request.RedisResponse
import dev.slne.surf.redis.request.RequestContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestHandlerAnnotationTest {
    @Test
    fun `test RequestHandler annotation can be applied to methods`() = runTest {
        val methods = TestHandler::class.java.declaredMethods
        val method = methods.find {
            it.name == "handleRequest" && it.isAnnotationPresent(HandleRedisRequest::class.java)
        }
        assertTrue(method != null, "RequestHandler method should be found")
        assertTrue(method.isAnnotationPresent(HandleRedisRequest::class.java))
    }

    @Serializable
    data class TestRequest(val data: String) : RedisRequest()

    @Serializable
    data class TestResponse(val result: String) : RedisResponse()

    object TestHandler {
        @HandleRedisRequest
        fun handleRequest(context: RequestContext<TestRequest>) {
            context.respond(TestResponse("processed: ${context.request.data}"))
        }
    }
}