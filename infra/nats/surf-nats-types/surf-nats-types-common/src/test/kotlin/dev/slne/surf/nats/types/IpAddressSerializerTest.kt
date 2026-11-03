package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IpAddressSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(IpAddressSerializer::class) InetAddress,
        @ProtoNumber(2) val nullable: @Serializable(IpAddressSerializer::class) InetAddress? = null,
    )

    private val ipv4 = InetAddress.ofLiteral("127.0.0.1")
    private val ipv6 = InetAddress.ofLiteral("2001:db8::1")

    @Test
    fun `descriptor is the surf IpAddress message`() {
        assertMessageDescriptor(IpAddressSerializer.descriptor, "surf.types.v1.IpAddress", "address" to 1)
    }

    @Test
    fun `ipv4 is four bytes`() {
        assertEquals("0a047f000001", encodeHex(IpAddressSerializer, ipv4))
        assertIs<Inet4Address>(decodeHex(IpAddressSerializer, "0a047f000001"))
        assertEquals(ipv4, decodeHex(IpAddressSerializer, "0a047f000001"))
    }

    @Test
    fun `ipv6 is sixteen bytes`() {
        val hex = "0a1020010db8000000000000000000000001"

        assertEquals(hex, encodeHex(IpAddressSerializer, ipv6))
        assertIs<Inet6Address>(decodeHex(IpAddressSerializer, hex))
        assertEquals(ipv6, decodeHex(IpAddressSerializer, hex))
    }

    @Test
    fun `host name is not written`() {
        val named = InetAddress.getByAddress("localhost", byteArrayOf(127, 0, 0, 1))

        assertEquals("0a047f000001", encodeHex(IpAddressSerializer, named))
    }

    @Test
    fun `nested and nullable fields`() {
        val holder = Holder(ipv4, ipv6)
        val hex = "0a060a047f000001" + "12120a1020010db8000000000000000000000001"

        assertEquals(hex, encodeHex(Holder.serializer(), holder))
        assertEquals(holder, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent address is a missing field`() {
        assertMissingField("surf.types.v1.IpAddress", "address") { decodeHex(IpAddressSerializer, "") }
        assertMissingField("surf.types.v1.IpAddress", "address") { decodeHex(Holder.serializer(), "0a00") }
    }

    @Test
    fun `address of the wrong length is rejected`() {
        assertInvalid { decodeHex(IpAddressSerializer, "0a00") }
        assertInvalid { decodeHex(IpAddressSerializer, "0a037f0000") }
        assertInvalid { decodeHex(IpAddressSerializer, "0a057f00000100") }
    }
}
