@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.serialization.java.ip.ipv4

import dev.slne.surf.api.serialization.java.ip.inet.InetAddressSerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.Inet4Address
import java.net.InetAddress

typealias SerializableInet4Address = @Serializable(
    with = Inet4AddressSerializer::class,
) Inet4Address

/**
 * Serializes an [Inet4Address] through the format's contextual [InetAddress] serializer, with
 * [InetAddressSerializer] as the fallback.
 *
 * [descriptor] always describes the fallback encoding. A format that registers its own
 * [InetAddress] serializer may write a different shape, which descriptor-driven checks such as
 * Json's map-key validation do not see.
 */
object Inet4AddressSerializer : KSerializer<Inet4Address> {
    private val inetAddressSerializer = ContextualSerializer(
        InetAddress::class,
        InetAddressSerializer,
        emptyArray(),
    )

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "surf.api.java.ip.Inet4Address",
        InetAddressSerializer.descriptor,
    )

    override fun serialize(
        encoder: Encoder,
        value: Inet4Address,
    ) {
        encoder.encodeSerializableValue(inetAddressSerializer, value)
    }

    override fun deserialize(decoder: Decoder): Inet4Address {
        val address = decoder.decodeSerializableValue(inetAddressSerializer)
        require(address is Inet4Address) { "Not an IPv4 address: $address" }

        return address
    }
}