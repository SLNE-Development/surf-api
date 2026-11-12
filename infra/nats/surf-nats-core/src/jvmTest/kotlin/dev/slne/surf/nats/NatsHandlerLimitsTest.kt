package dev.slne.surf.nats

import dev.slne.surf.nats.config.HandlerLimits
import dev.slne.surf.nats.rpc.RpcDefaults
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NatsHandlerLimitsTest {

    @AfterEach
    fun restoreDefaults() = NatsHandlerLimits.setDefaults(RpcDefaults.MAX_CONCURRENT_HANDLERS)

    @Test
    fun `long share derived from a pool of one still admits long calls`() {
        NatsHandlerLimits.setDefaults(maxConcurrentHandlers = 2, maxConcurrentLongHandlers = 1 / 2)
        assertEquals(HandlerLimits(2, 1), NatsHandlerLimits.defaults)
    }

    @Test
    fun `long share above the total is lowered to the total`() {
        NatsHandlerLimits.setDefaults(maxConcurrentHandlers = 4, maxConcurrentLongHandlers = 6)
        assertEquals(HandlerLimits(4, 4), NatsHandlerLimits.defaults)
    }

    @Test
    fun `total must be positive`() {
        assertFailsWith<IllegalArgumentException> { NatsHandlerLimits.setDefaults(0) }
    }
}
