package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.protocol.ProtocolHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

class NatsHeadersTest {

    // PayloadStore counts a header it adds after the size check with lineBytes
    @Test
    fun lineBytesMatchTheEncodedHeaders() {
        val headers = NatsHeaders.forOrigin("surf-panel@0b6f4c1e-0c6e-4d7b-9a51-3f0c2a7e9d10")
        var expected = NatsHeaders.serializedLength(headers)

        val lines = listOf(
            ProtocolHeaders.REPLY_TO to "_INBOX.4ZQ8XGJ3W2SE1OP0V5Y7AB",
            ProtocolHeaders.NATS_MSG_ID to "5d1f0c7a-2b8e-4f43-a6c1-90e2d3b4c5f6",
            ProtocolHeaders.NATS_TTL to "60s",
            ProtocolHeaders.DEADLINE to "1761234567890",
        )
        for ((name, value) in lines) {
            headers.set(name, value)
            expected += NatsHeaders.lineBytes(name, value)
            assertEquals(expected, NatsHeaders.serializedLength(headers))
        }

        assertEquals(0, NatsHeaders.lineBytes(ProtocolHeaders.DEADLINE, null))
        assertEquals(0, NatsHeaders.lineBytes(ProtocolHeaders.DEADLINE, ""))
    }
}
