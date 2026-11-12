package dev.slne.surf.nats.config

import dev.slne.surf.api.config.type.number.IntOr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HandlerLimitsTest {

    private val defaults = HandlerLimits(total = 20, long = 5)

    @Test
    fun `long share defaults to half of the total and at least one`() {
        assertEquals(HandlerLimits(20, 10), HandlerLimits(20))
        assertEquals(HandlerLimits(1, 1), HandlerLimits(1))
    }

    @Test
    fun `limits outside their range are rejected`() {
        assertFailsWith<IllegalArgumentException> { HandlerLimits(0) }
        assertFailsWith<IllegalArgumentException> { HandlerLimits(4, long = 0) }
        assertFailsWith<IllegalArgumentException> { HandlerLimits(4, long = 5) }
    }

    @Test
    fun `defaults apply when nothing is configured`() {
        assertEquals(defaults, NatsConfig().handlerLimits("svc", defaults))
    }

    @Test
    fun `configured total without a long share gets half of it`() {
        val config = NatsConfig(maxConcurrentHandlers = IntOr.Default(8))

        assertEquals(HandlerLimits(8, 4), config.handlerLimits("svc", defaults))
    }

    @Test
    fun `configured long share keeps the default total`() {
        val config = NatsConfig(maxConcurrentLongHandlers = IntOr.Default(3))

        assertEquals(HandlerLimits(20, 3), config.handlerLimits("svc", defaults))
    }

    @Test
    fun `service entries take precedence over global values`() {
        val config = NatsConfig(
            maxConcurrentHandlers = IntOr.Default(8),
            maxConcurrentLongHandlers = IntOr.Default(6),
            maxConcurrentHandlersByService = mapOf("svc" to 4),
            maxConcurrentLongHandlersByService = mapOf("svc" to 1),
        )

        assertEquals(HandlerLimits(4, 1), config.handlerLimits("svc", defaults))
        assertEquals(HandlerLimits(8, 6), config.handlerLimits("other", defaults))
    }

    @Test
    fun `global long share above the total of a service is lowered to that total`() {
        val config = NatsConfig(
            maxConcurrentLongHandlers = IntOr.Default(6),
            maxConcurrentHandlersByService = mapOf("svc" to 4),
        )

        assertEquals(HandlerLimits(4, 4), config.handlerLimits("svc", defaults))
    }

    @Test
    fun `long share above its configured total fails validation`() {
        assertFailsWith<IllegalStateException> {
            val config = NatsConfig(maxConcurrentHandlers = IntOr.Default(4), maxConcurrentLongHandlers = IntOr.Default(5))
            config.validate()
        }
        assertFailsWith<IllegalStateException> {
            NatsConfig(
                maxConcurrentHandlersByService = mapOf("svc" to 2),
                maxConcurrentLongHandlersByService = mapOf("svc" to 3),
            ).validate()
        }
        assertFailsWith<IllegalStateException> {
            NatsConfig(maxConcurrentLongHandlersByService = mapOf("svc" to 0)).validate()
        }
    }
}
