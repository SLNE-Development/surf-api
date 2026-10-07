package dev.slne.surf.nats.codec

import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.connection.NatsConnectionManager
import kotlin.test.Test
import kotlin.test.assertEquals

class NatsHeadersTest {

    @Test
    fun `base headers contain content type, origin and version`() {
        val headers = NatsHeaders.base("transaction", WireFormat.JSON)

        assertEquals(WireFormat.JSON.contentType, headers.getFirst(NatsHeaders.CONTENT_TYPE))
        assertEquals("transaction@${NatsConnectionManager.instanceId}", headers.getFirst(NatsHeaders.ORIGIN))
        assertEquals(NatsConstants.VERSION, headers.getFirst(NatsHeaders.SENDER_VERSION))
    }
}
