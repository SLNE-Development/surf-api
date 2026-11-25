package dev.slne.surf.api.serialization.java.ip.inet

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.InetAddress

typealias SerializableBytesInetAddress = @Serializable(
    with = InetAddressBytesSerializer::class,
) InetAddress

/**
 * Serializes an [InetAddress] as its raw address in network byte order: 4 bytes for IPv4, 16 bytes
 * for IPv6.
 *
 * The host name and the IPv6 scope are not written, so a decoded address has neither and decoding
 * never performs a DNS lookup. Decoding throws [SerializationException] for any other length.
 */
object InetAddressBytesSerializer : KSerializer<InetAddress> {
    private const val IPV4_BYTES = 4
    private const val IPV6_BYTES = 16

    private val byteArraySerializer = ByteArraySerializer()

    override val descriptor = SerialDescriptor(
        "surf.api.java.ip.InetAddressBytes",
        byteArraySerializer.descriptor,
    )

    override fun serialize(encoder: Encoder, value: InetAddress) {
        encoder.encodeSerializableValue(byteArraySerializer, value.address)
    }

    override fun deserialize(decoder: Decoder): InetAddress {
        val bytes = decoder.decodeSerializableValue(byteArraySerializer)

        if (bytes.size != IPV4_BYTES && bytes.size != IPV6_BYTES) {
            throw SerializationException(
                "An IP address must be $IPV4_BYTES or $IPV6_BYTES bytes, was ${bytes.size}",
            )
        }

        return InetAddress.getByAddress(bytes)
    }
}
