@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.serialization.java.ip.ipv6

import dev.slne.surf.api.serialization.java.ip.inet.InetAddressSerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.Inet6Address
import java.net.InetAddress

typealias SerializableInet6Address = @Serializable(
    with = Inet6AddressSerializer::class,
) Inet6Address

/**
 * Serializes an [Inet6Address] through the format's contextual [InetAddress] serializer, with
 * [InetAddressSerializer] as the fallback.
 *
 * [descriptor] always describes the fallback encoding. A format that registers its own
 * [InetAddress] serializer may write a different shape, which descriptor-driven checks such as
 * Json's map-key validation do not see.
 */
object Inet6AddressSerializer : KSerializer<Inet6Address> {
    private val inetAddressSerializer = ContextualSerializer(
        InetAddress::class,
        InetAddressSerializer,
        emptyArray(),
    )

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "surf.api.java.ip.Inet6Address",
        InetAddressSerializer.descriptor,
    )

    override fun serialize(
        encoder: Encoder,
        value: Inet6Address,
    ) {
        encoder.encodeSerializableValue(inetAddressSerializer, value)
    }

    override fun deserialize(decoder: Decoder): Inet6Address {
        val address = decoder.decodeSerializableValue(inetAddressSerializer)
        require(address is Inet6Address) { "Not an IPv6 address: $address" }

        return address
    }
}