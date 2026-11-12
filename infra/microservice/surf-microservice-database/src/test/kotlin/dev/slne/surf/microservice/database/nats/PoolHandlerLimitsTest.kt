package dev.slne.surf.microservice.database.nats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PoolHandlerLimitsTest {

    @Test
    fun `allows twice the pool size and half of it for long-running calls`() {
        assertEquals(PoolHandlerLimits(total = 20, long = 5), PoolHandlerLimits.forPoolSize(10))
        assertEquals(PoolHandlerLimits(total = 6, long = 1), PoolHandlerLimits.forPoolSize(3))
        assertEquals(PoolHandlerLimits(total = 4, long = 1), PoolHandlerLimits.forPoolSize(2))
    }

    @Test
    fun `keeps one long-running handler for a pool of one`() {
        assertEquals(PoolHandlerLimits(total = 2, long = 1), PoolHandlerLimits.forPoolSize(1))
    }

    @Test
    fun `rejects a pool without connections`() {
        assertFailsWith<IllegalArgumentException> { PoolHandlerLimits.forPoolSize(0) }
    }
}
