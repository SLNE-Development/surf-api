@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.inet

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.InetAddress

typealias SerializableInetAddress = @Serializable(with = InetAddressSerializer::class) InetAddress

object InetAddressSerializer : KSerializer<InetAddress> {
    override val descriptor =
        PrimitiveSerialDescriptor("surfapi.java.ip.InetAddress", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: InetAddress,
    ) {
        encoder.encodeString(value.hostAddress)
    }

    override fun deserialize(decoder: Decoder): InetAddress {
        return InetAddress.getByName(decoder.decodeString())
    }
}