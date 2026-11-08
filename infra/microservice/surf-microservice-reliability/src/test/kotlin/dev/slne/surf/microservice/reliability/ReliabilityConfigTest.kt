package dev.slne.surf.microservice.reliability

import dev.slne.surf.nats.rpc.NatsPersistentLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration

class ReliabilityConfigTest {

    @Test
    fun `stored replies are kept for the redelivery horizon of surf-nats by default`() {
        assertEquals(NatsPersistentLimits.REDELIVERY_HORIZON, ReliabilityConfig().atomicOperations.retention)
    }

    @Test
    fun `rejects a cleanup interval that is not positive`() {
        assertFailsWith<IllegalArgumentException> { ReliabilityConfig(cleanupInterval = Duration.ZERO) }
    }
}
