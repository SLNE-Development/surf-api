package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.protocol.PayloadBucket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CallFlowTest {

    @Test
    fun flowsFollowPersistenceAndResult() {
        for (kind in NatsResultKind.entries) assertEquals(CallFlow.CORE, CallFlow.of(persistent = false, kind))
        assertEquals(CallFlow.FIRE, CallFlow.of(persistent = true, NatsResultKind.UNIT))
        assertEquals(CallFlow.PERSISTENT_RPC, CallFlow.of(persistent = true, NatsResultKind.VALUE))
    }

    // Spec 12.6
    @Test
    fun wireContractPerFlow() {
        assertEquals(PayloadBucket.TRANSIENT, CallFlow.CORE.bucket)
        assertTrue(CallFlow.CORE.sendsDeadline)
        assertFalse(CallFlow.CORE.sendsReplyTo)
        assertFalse(CallFlow.CORE.sendsTtl)
        assertEquals(5.seconds, CallFlow.CORE.offloadLifetime(5.seconds))

        assertEquals(PayloadBucket.PERSISTENT, CallFlow.FIRE.bucket)
        assertFalse(CallFlow.FIRE.sendsDeadline)
        assertFalse(CallFlow.FIRE.sendsReplyTo)
        assertFalse(CallFlow.FIRE.sendsTtl)
        assertEquals(7.days, CallFlow.FIRE.offloadLifetime(5.seconds))

        assertEquals(PayloadBucket.TRANSIENT, CallFlow.PERSISTENT_RPC.bucket)
        assertTrue(CallFlow.PERSISTENT_RPC.sendsDeadline)
        assertTrue(CallFlow.PERSISTENT_RPC.sendsReplyTo)
        assertTrue(CallFlow.PERSISTENT_RPC.sendsTtl)
        assertEquals(60.seconds, CallFlow.PERSISTENT_RPC.offloadLifetime(60.seconds))
    }

    @Test
    fun fireTimeoutsMustFitTheDuplicateWindow() {
        PersistentRules.requireFireTimeout(2.minutes, 2.minutes) { "a.B.fire" }
        val error = assertFailsWith<IllegalArgumentException> {
            PersistentRules.requireFireTimeout(121.seconds, 2.minutes) { "a.B.fire" }
        }
        assertEquals(
            "Timeout 2m 1s for persistent a.B.fire without a result exceeds the JetStream duplicate window of 2m",
            error.message,
        )
    }

    @Test
    fun publishPhaseIsCappedByTheDuplicateWindow() {
        assertEquals(30.seconds, PersistentRules.publishPhase(30.seconds, 2.minutes))
        assertEquals(2.minutes, PersistentRules.publishPhase(2.minutes, 2.minutes))
        assertEquals(2.minutes, PersistentRules.publishPhase(10.minutes, 2.minutes))
    }
}
