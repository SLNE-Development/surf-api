package de.slne.redis.request

import de.slne.redis.RedisApi
import de.slne.redis.RedisTestBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RequestResponseBusTest : RedisTestBase() {

    @Serializable
    data class TestRequest(val message: String) : RedisRequest()

    @Serializable
    data class TestResponse(val result: String) : RedisResponse()

    override fun beforeApiFreeze(api: RedisApi) {
        api.registerRequestHandler(TestHandler)
    }

    @Test
    fun `test response bus`() = runBlocking {
        val request = TestRequest("hello world")
        val response = redisApi.sendRequest<TestResponse>(request)

        assertEquals("handled: hello world", response.result)
    }

    object TestHandler {
        @HandleRedisRequest
        fun handleTestRequest(context: RequestContext<TestRequest>) {
            context.respond(TestResponse("handled: ${context.request.message}"))
        }
    }
}