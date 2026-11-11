package dev.slne.surf.nats.rpc.delivery

import dev.slne.surf.nats.rpc.NatsRedeliveryException
import io.nats.client.support.NatsJetStreamClientError
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

class DeliveryCauseTest {

    @Test
    fun `only a missing payload object makes a load failure permanent`() {
        val notFound = NatsJetStreamClientError.OsObjectNotFound.instance()
        assertEquals(DeliveryCause.PAYLOAD_MISSING, DeliveryCause.ofLoadFailure(notFound))
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofLoadFailure(IllegalStateException(notFound.message)))

        // [OS-90206] also covers a slow read, which a later delivery may not hit
        val chunkMismatch = NatsJetStreamClientError.OsGetChunksMismatch.instance()
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofLoadFailure(chunkMismatch))
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofLoadFailure(IOException("connection lost")))
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofLoadFailure(CancellationException("cancelled")))
    }

    @Test
    fun `decode failure is permanent unless it is a cancellation`() {
        assertEquals(DeliveryCause.UNDECODABLE, DeliveryCause.ofDecodeFailure(SerializationException("truncated")))
        assertEquals(DeliveryCause.UNDECODABLE, DeliveryCause.ofDecodeFailure(NatsRedeliveryException("retry")))
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofDecodeFailure(CancellationException("cancelled")))
    }

    @Test
    fun `only a redelivery exception requests a redelivery`() {
        assertEquals(DeliveryCause.REDELIVERY_REQUESTED, DeliveryCause.ofFailure(NatsRedeliveryException("retry")))
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofFailure(IllegalStateException("failed")))
        assertEquals(DeliveryCause.FAILED, DeliveryCause.ofFailure(CancellationException("cancelled")))
    }

    @Test
    fun `payload, decode and reply causes are permanent`() {
        val permanent = setOf(
            DeliveryCause.PAYLOAD_MISSING,
            DeliveryCause.UNDECODABLE,
            DeliveryCause.STORED_ERROR,
            DeliveryCause.REPLY_MISMATCH,
        )
        assertEquals(permanent, DeliveryCause.entries.filter { it.permanent }.toSet())
    }
}
