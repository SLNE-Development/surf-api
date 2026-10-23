package dev.slne.surf.nats.js

import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

class OutgoingHeadersTest {

    @Test
    fun bytesMatchTheEncodedHeaders() {
        val origin = "surf-panel@0b6f4c1e-0c6e-4d7b-9a51-3f0c2a7e9d10"
        val headers = OutgoingHeaders(Wire.baseHeaders(origin), Wire.headerBytes(Wire.baseHeaders(origin)))
        assertEquals(Wire.headerBytes(headers.msgHdrs), headers.bytes)

        headers.set(ProtocolHeaders.REPLY_TO, "_INBOX.4ZQ8XGJ3W2SE1OP0V5Y7AB")
        headers.set(ProtocolHeaders.NATS_MSG_ID, "5d1f0c7a-2b8e-4f43-a6c1-90e2d3b4c5f6")
        headers.set(ProtocolHeaders.NATS_TTL, "60s")
        headers.set(ProtocolHeaders.DEADLINE, "1761234567890")
        assertEquals(Wire.headerBytes(headers.msgHdrs), headers.bytes)
    }
}
