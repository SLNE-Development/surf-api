package dev.slne.surf.api.minestom.server.chat.impl

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TickThrottlerTest {

    @Test
    fun `a threshold of zero never throttles`() {
        val throttler = TickThrottler(20, 0)
        repeat(100) { assertTrue(throttler.isIncrementAndUnderThreshold()) }
        assertTrue(throttler.isUnderThreshold())
    }

    @Test
    fun `the counter throttles once it reaches the threshold`() {
        val throttler = TickThrottler(20, 60)
        assertTrue(throttler.isIncrementAndUnderThreshold())
        assertTrue(throttler.isIncrementAndUnderThreshold())
        assertFalse(throttler.isIncrementAndUnderThreshold())
    }

    @Test
    fun `ticks drain the counter below the threshold again`() {
        val throttler = TickThrottler(20, 60)
        repeat(3) { throttler.increment() }
        assertFalse(throttler.isUnderThreshold())

        throttler.tick()
        assertTrue(throttler.isUnderThreshold())
    }
}
