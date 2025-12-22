package dev.slne.redis

import com.redis.testcontainers.RedisContainer
import io.lettuce.core.RedisURI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class RedisTestBase {
    companion object {
        @JvmStatic
        @Container
        val redisContainer =
            RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG))
    }

    lateinit var redisApi: RedisApi

    @BeforeEach
    fun setUpRedisApi() {
        val api = RedisApi.create(RedisURI.create(redisContainer.redisURI))
        beforeApiFreeze(api)
        api.freezeAndConnect()
        redisApi = api
    }

    open fun beforeApiFreeze(api: RedisApi) = Unit

    @AfterEach
    fun tearDownRedisApi() {
        if (::redisApi.isInitialized) {
            redisApi.disconnect()
        }
    }
}