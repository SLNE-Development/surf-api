package dev.slne.surf.redis

import kotlinx.coroutines.test.runTest
import kotlin.test.assertTrue

class RedisSmokeTest : RedisTestBase() {

    //    @Test
    fun `redis container responds to ping`() = runTest {
        assertTrue(redisApi.isAlive())
    }
}