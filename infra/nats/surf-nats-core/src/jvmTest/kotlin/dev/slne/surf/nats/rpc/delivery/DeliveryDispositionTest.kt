package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.rpc.RpcDefaults
import dev.slne.surf.nats.rpc.delivery.DeliveryDisposition.Ack
import dev.slne.surf.nats.rpc.delivery.DeliveryDisposition.DeadLetter
import dev.slne.surf.nats.rpc.delivery.DeliveryDisposition.Nak
import dev.slne.surf.nats.rpc.delivery.DeliveryDisposition.Term
import dev.slne.surf.nats.runtime.rpc.CallFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DeliveryDispositionTest {

    private val backoff = listOf(1.seconds, 5.seconds)
    private val last = RpcDefaults.MAX_DELIVER.toLong()

    private val permanentFailures = DeliveryCause.entries.filter { it.permanent }
    private val transientFailures =
        DeliveryCause.entries - permanentFailures - setOf(DeliveryCause.COMPLETED, DeliveryCause.NOT_ADMITTED)
    private val callerGaveUp = setOf(DeliveryCause.TIMED_OUT, DeliveryCause.NOT_ADMITTED)

    @Test
    fun `completed fire is acknowledged`() {
        assertEquals(Ack, fire(DeliveryCause.COMPLETED, 1))
        assertEquals(Ack, fire(DeliveryCause.COMPLETED, last))
    }

    @Test
    fun `transient fire failure is redelivered with the backoff of its delivery`() {
        for (cause in transientFailures) {
            assertEquals(Nak(1.seconds), fire(cause, 1), "$cause")
            assertEquals(Nak(5.seconds), fire(cause, 2), "$cause")
            assertEquals(Nak(5.seconds), fire(cause, last - 1), "$cause: the last backoff entry repeats")
        }
    }

    @Test
    fun `transient fire failure is dead-lettered on the last delivery`() {
        for (cause in transientFailures) {
            assertEquals(DeadLetter(5.seconds), fire(cause, last), "$cause")
            assertEquals(DeadLetter(5.seconds), fire(cause, last + 1), "$cause")
        }
    }

    @Test
    fun `permanent fire failure is dead-lettered on every delivery`() {
        for (cause in permanentFailures) {
            assertEquals(DeadLetter(1.seconds), fire(cause, 1), "$cause")
            assertEquals(DeadLetter(5.seconds), fire(cause, 2), "$cause")
        }
    }

    @Test
    fun `fire without a handler slot is redelivered and never dead-lettered`() {
        assertEquals(Nak(1.seconds), fire(DeliveryCause.NOT_ADMITTED, 1))
        assertEquals(Nak(5.seconds), fire(DeliveryCause.NOT_ADMITTED, last))
    }

    @Test
    fun `persistent rpc is acknowledged after every reply and failure`() {
        for (cause in DeliveryCause.entries - callerGaveUp - DeliveryCause.REDELIVERY_REQUESTED) {
            assertEquals(Ack, rpc(cause, 1), "$cause")
            assertEquals(Ack, rpc(cause, last, timeLeft = Duration.ZERO), "$cause")
        }
    }

    @Test
    fun `persistent rpc whose caller gave up is terminated`() {
        for (cause in callerGaveUp) {
            assertEquals(Term, rpc(cause, 1), "$cause")
            assertEquals(Term, rpc(cause, last), "$cause")
        }
    }

    @Test
    fun `requested redelivery happens when its backoff ends before the deadline`() {
        val justEnough = 1.seconds + 1.milliseconds
        assertEquals(Nak(1.seconds), rpc(DeliveryCause.REDELIVERY_REQUESTED, 1, timeLeft = justEnough))
        assertEquals(Nak(5.seconds), rpc(DeliveryCause.REDELIVERY_REQUESTED, 2, timeLeft = 5.seconds + 1.milliseconds))
        assertEquals(Nak(5.seconds), rpc(DeliveryCause.REDELIVERY_REQUESTED, last - 1))
    }

    @Test
    fun `requested redelivery that cannot happen before the deadline is answered`() {
        assertEquals(Ack, rpc(DeliveryCause.REDELIVERY_REQUESTED, 1, timeLeft = 1.seconds), "ends at the deadline")
        assertEquals(Ack, rpc(DeliveryCause.REDELIVERY_REQUESTED, 1, timeLeft = 500.milliseconds))
        assertEquals(
            Ack,
            rpc(DeliveryCause.REDELIVERY_REQUESTED, 2, timeLeft = 3.seconds),
            "compares the backoff of this delivery"
        )
    }

    @Test
    fun `requested redelivery on the last delivery is answered`() {
        assertEquals(Ack, rpc(DeliveryCause.REDELIVERY_REQUESTED, last))
    }

    @Test
    fun `core call has no delivery`() {
        assertFailsWith<IllegalStateException> {
            DeliveryDisposition.of(DeliveryCause.COMPLETED, CallFlow.CORE, 1, Duration.INFINITE, backoff)
        }
    }

    private fun fire(cause: DeliveryCause, delivered: Long): DeliveryDisposition =
        DeliveryDisposition.of(cause, CallFlow.FIRE, delivered, Duration.INFINITE, backoff)

    private fun rpc(
        cause: DeliveryCause,
        delivered: Long,
        timeLeft: Duration = Duration.INFINITE,
    ): DeliveryDisposition = DeliveryDisposition.of(cause, CallFlow.PERSISTENT_RPC, delivered, timeLeft, backoff)
}
