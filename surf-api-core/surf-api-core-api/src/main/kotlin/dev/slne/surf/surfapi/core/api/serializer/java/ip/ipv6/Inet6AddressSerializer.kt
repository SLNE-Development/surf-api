@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv6

import dev.slne.surf.surfapi.core.api.serializer.java.ip.inet.InetAddressSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.Inet6Address

typealias SerializableInet6Address = @Serializable(with = Inet6AddressSerializer::class) Inet6Address

object Inet6AddressSerializer : KSerializer<Inet6Address> {
    override val descriptor =
        SerialDescriptor("surfapi.java.ip.Inet6Address", InetAddressSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Inet6Address,
    ) {
        encoder.encodeSerializableValue(InetAddressSerializer, value)
    }

    override fun deserialize(decoder: Decoder): Inet6Address {
        val address = decoder.decodeSerializableValue(InetAddressSerializer)
        require(address is Inet6Address) { "Not an IPv6 address: $address" }
        return address
    }
}