package dev.slne.surf.nats.types

import dev.slne.surf.api.serialization.java.uuid.JavaUUIDSerializer
import java.util.*

/**
 * Serializes a [UUID] as the protobuf message `surf.types.v1.Uuid`.
 *
 * Field `value` (1, required) holds the 16 big-endian bytes written by [JavaUUIDSerializer]: the
 * most significant 64 bits, then the least significant 64 bits. The `dev.slne.surf.nats` Gradle
 * plugin maps fields of this message type to [UUID] with this serializer.
 *
 * @throws kotlinx.serialization.MissingFieldException on decoding when `value` is absent
 * @throws kotlinx.serialization.SerializationException on decoding when `value` is not 16 bytes
 */
object UuidSerializer : ProtoWrapperSerializer<UUID>("surf.types.v1.Uuid", JavaUUIDSerializer)
