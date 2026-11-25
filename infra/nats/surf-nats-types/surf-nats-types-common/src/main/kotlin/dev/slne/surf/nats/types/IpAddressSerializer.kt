package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.java.ip.inet.InetAddressBytesSerializer
import java.net.InetAddress

/**
 * Serializes an [InetAddress] as the protobuf message `surf.types.v1.IpAddress`.
 *
 * Field `address` (1, required) holds the raw address in network byte order as written by
 * [InetAddressBytesSerializer]: 4 bytes for IPv4, 16 bytes for IPv6. Host name and IPv6 scope are
 * not written, so a decoded address has neither. The `dev.slne.surf.nats` Gradle plugin maps fields
 * of this message type to [InetAddress] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `address` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `address` is neither 4
 *   nor 16 bytes
 */
object IpAddressSerializer : ProtoWrapperSerializer<InetAddress>(
    "surf.types.v1.IpAddress",
    InetAddressBytesSerializer,
    fieldName = "address",
)
