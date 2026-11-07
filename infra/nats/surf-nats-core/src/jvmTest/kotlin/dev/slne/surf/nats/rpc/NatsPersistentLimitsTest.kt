package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class NatsPersistentLimitsTest {

    @Test
    fun `public limits match the protocol retention`() {
        assertEquals(ProtocolDefaults.PERSIST_MAX_AGE, NatsPersistentLimits.MESSAGE_MAX_AGE)
        assertEquals(ProtocolDefaults.PERSIST_MAX_AGE, NatsPersistentLimits.DEAD_LETTER_MAX_AGE)
        assertEquals(ProtocolDefaults.PERSISTENT_PAYLOAD_TTL_SECONDS.seconds, NatsPersistentLimits.MESSAGE_MAX_AGE)
        assertEquals(14.days, NatsPersistentLimits.REDELIVERY_HORIZON)
    }
}
