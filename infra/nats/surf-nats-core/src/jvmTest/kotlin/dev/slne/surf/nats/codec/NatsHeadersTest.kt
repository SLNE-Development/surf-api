package dev.slne.surf.nats.codec

import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

class NatsHeadersTest {

    @Test
    fun `base headers contain the protobuf content type, origin and version`() {
        val headers = NatsHeaders.base("transaction")

        assertEquals("application/protobuf", headers.getFirst(ProtocolHeaders.CONTENT_TYPE))
        assertEquals("transaction@${NatsConnectionManager.instanceId}", headers.getFirst(ProtocolHeaders.ORIGIN))
        assertEquals(NatsConstants.VERSION, headers.getFirst(ProtocolHeaders.SENDER_VERSION))
    }
}
