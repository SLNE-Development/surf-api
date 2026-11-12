package dev.slne.surf.microservice.database.nats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoolSizedHandlerLimitsTest {
    private val applied = mutableListOf<PoolHandlerLimits>()

    @Test
    fun `sets the limits derived from the pool size when surf-nats is present`() {
        limits(natsPresent = true).apply(poolMaxSize = 10)

        assertEquals(listOf(PoolHandlerLimits(total = 20, long = 5)), applied)
    }

    @Test
    fun `keeps the surf-nats defaults when the pool size is unknown`() {
        limits(natsPresent = true).apply(poolMaxSize = null)

        assertTrue(applied.isEmpty())
    }

    @Test
    fun `does nothing without surf-nats`() {
        limits(natsPresent = false).apply(poolMaxSize = 10)

        assertTrue(applied.isEmpty())
    }

    @Test
    fun `keeps the surf-nats defaults when the surf-nats version cannot take them`() {
        val incompatible = PoolSizedHandlerLimits({ true }) { throw NoSuchMethodError("setDefaults(II)V") }

        incompatible.apply(poolMaxSize = 10)
    }

    @Test
    fun `does not link surf-nats when it is missing from the class path`() {
        assertFalse(isSurfNatsPresent(), "surf-nats must not be on the test runtime class path of this module")

        PoolSizedHandlerLimits().apply(poolMaxSize = 10)
    }

    private fun limits(natsPresent: Boolean) = PoolSizedHandlerLimits({ natsPresent }, applied::add)
}
