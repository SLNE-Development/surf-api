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

/**
 * Serializes an [InetSocketAddress] as the protobuf message `surf.types.v1.InetSocketAddress`.
 *
 * The message has the following fields:
 * - `address` (1, optional): the resolved IP address as `surf.types.v1.IpAddress`, see [IpAddressSerializer]. Unset
 *   for an unresolved socket address.
 * - `host` (2, required): [InetSocketAddress.getHostString], a host name or an address literal.
 * - `port` (3, required): the port, 0 to 65535.
 *
 * A resolved address decodes with its IP address and `host` as host name, without a DNS lookup. An address without
 * `address` decodes as an unresolved socket address. The `dev.slne.surf.nats` Gradle plugin maps fields of this
 * message type to [InetSocketAddress] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `host` or `port` is absent and
 * [kotlinx.serialization.SerializationException] when the port is out of range or `address` is invalid.
 */
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
