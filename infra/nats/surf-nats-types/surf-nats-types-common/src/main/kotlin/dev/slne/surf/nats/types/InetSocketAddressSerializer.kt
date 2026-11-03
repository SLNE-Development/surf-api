package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.net.InetAddress
import java.net.InetSocketAddress

object InetSocketAddressSerializer : KSerializer<InetSocketAddress> {
    private const val MAX_PORT = 65535

    override val descriptor: SerialDescriptor = InetSocketAddressProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: InetSocketAddress) {
        encoder.encodeSerializableValue(
            InetSocketAddressProto.serializer(),
            InetSocketAddressProto(value.address, value.hostString, value.port)
        )
    }

    override fun deserialize(decoder: Decoder): InetSocketAddress {
        val proto = decoder.decodeSerializableValue(InetSocketAddressProto.serializer())
        if (proto.port !in 0..MAX_PORT) {
            throw SerializationException("${descriptor.serialName}.port must be in 0..$MAX_PORT, was ${proto.port}")
        }

        val address = proto.address ?: return InetSocketAddress.createUnresolved(proto.host, proto.port)

        return InetSocketAddress(InetAddress.getByAddress(proto.host, address.address), proto.port)
    }
}

@Serializable
@SerialName("surf.types.v1.InetSocketAddress")
private class InetSocketAddressProto(
    @ProtoNumber(1) val address: @Serializable(with = IpAddressSerializer::class) InetAddress? = null,
    @ProtoNumber(2) val host: String,
    @ProtoNumber(3) val port: Int,
)
