package dev.slne.surf.nats.types

import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InetSocketAddressSerializerTest {

    @Test
    fun `descriptor is the surf InetSocketAddress message`() {
        assertMessageDescriptor(
            InetSocketAddressSerializer.descriptor,
            "surf.types.v1.InetSocketAddress",
            "address" to 1, "host" to 2, "port" to 3,
        )
    }

    @Test
    fun `resolved address keeps address, host name and port`() {
        val value = InetSocketAddress(InetAddress.getByAddress("localhost", byteArrayOf(127, 0, 0, 1)), 25565)
        val hex = "0a060a047f000001" + "12096c6f63616c686f7374" + "18ddc701"

        assertEquals(hex, encodeHex(InetSocketAddressSerializer, value))
        val decoded = decodeHex(InetSocketAddressSerializer, hex)
        assertEquals(value, decoded)
        assertEquals("localhost", decoded.hostString)
        assertFalse(decoded.isUnresolved)
    }

    @Test
    fun `address literal keeps the literal as host string`() {
        val value = InetSocketAddress(InetAddress.ofLiteral("10.0.0.1"), 0)
        val hex = "0a060a040a000001" + "120831302e302e302e31" + "1800"

        assertEquals(hex, encodeHex(InetSocketAddressSerializer, value))
        assertEquals(value, decodeHex(InetSocketAddressSerializer, hex))
    }

    @Test
    fun `unresolved address stays unresolved`() {
        val value = InetSocketAddress.createUnresolved("example.org", 4222)
        val hex = "120b6578616d706c652e6f7267" + "18fe20"

        assertEquals(hex, encodeHex(InetSocketAddressSerializer, value))
        val decoded = decodeHex(InetSocketAddressSerializer, hex)
        assertEquals(value, decoded)
        assertTrue(decoded.isUnresolved)
    }

    @Test
    fun `absent host or port is a missing field`() {
        assertMissingField("surf.types.v1.InetSocketAddress", "host", "port") {
            decodeHex(InetSocketAddressSerializer, "")
        }
    }

    @Test
    fun `port out of range is rejected`() {
        assertInvalid { decodeHex(InetSocketAddressSerializer, "120b6578616d706c652e6f7267" + "18808004") } // 65536
        assertInvalid { decodeHex(InetSocketAddressSerializer, "120b6578616d706c652e6f7267" + "18ffffffffffffffffff01") }
    }
}
