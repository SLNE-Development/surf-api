package dev.slne.redis

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class RedisSmokeTest : RedisTestBase() {

    @Test
    fun `redis container responds to ping`() = runTest {
        assertTrue(redisApi.isAlive())
    }
}