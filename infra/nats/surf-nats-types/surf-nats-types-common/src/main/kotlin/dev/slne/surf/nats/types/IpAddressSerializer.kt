package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.net.InetAddress

object IpAddressSerializer : KSerializer<InetAddress> {
    private const val IPV4_BYTES = 4
    private const val IPV6_BYTES = 16

    override val descriptor: SerialDescriptor = IpAddressProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: InetAddress) {
        encoder.encodeSerializableValue(IpAddressProto.serializer(), IpAddressProto(value.address))
    }

    override fun deserialize(decoder: Decoder): InetAddress {
        val bytes = decoder.decodeSerializableValue(IpAddressProto.serializer()).address
            ?: throw MissingFieldException("address", descriptor.serialName)
        if (bytes.size != IPV4_BYTES && bytes.size != IPV6_BYTES) {
            throw SerializationException(
                "${descriptor.serialName}.address must be $IPV4_BYTES or $IPV6_BYTES bytes, was ${bytes.size}"
            )
        }

        return InetAddress.getByAddress(bytes)
    }
}

@Serializable
@SerialName("surf.types.v1.IpAddress")
private class IpAddressProto(
    @ProtoNumber(1) val address: ByteArray? = null,
)
