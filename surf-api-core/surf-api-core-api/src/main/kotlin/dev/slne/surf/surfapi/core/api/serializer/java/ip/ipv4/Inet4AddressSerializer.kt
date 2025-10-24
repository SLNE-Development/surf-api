@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv4

import dev.slne.surf.surfapi.core.api.serializer.java.ip.inet.InetAddressSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.Inet4Address

typealias SerializableInet4Address = @Serializable(with = Inet4AddressSerializer::class) Inet4Address

object Inet4AddressSerializer : KSerializer<Inet4Address> {
    override val descriptor =
        SerialDescriptor("surfapi.java.ip.Inet4Address", InetAddressSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Inet4Address,
    ) {
        encoder.encodeSerializableValue(InetAddressSerializer, value)
    }

    override fun deserialize(decoder: Decoder): Inet4Address {
        val address = decoder.decodeSerializableValue(InetAddressSerializer)
        require(address is Inet4Address) { "Not an IPv4 address: $address" }
        return address
    }
}