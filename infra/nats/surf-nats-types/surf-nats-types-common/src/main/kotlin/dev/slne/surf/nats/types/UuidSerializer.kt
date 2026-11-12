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
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Serializes a [UUID] as the protobuf message `surf.types.v1.Uuid`.
 *
 * Field `value` (1, required) holds exactly 16 bytes, big-endian: the most significant 64 bits, then the least
 * significant 64 bits. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [UUID] with this
 * serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `value` is absent and
 * [kotlinx.serialization.SerializationException] when it is not 16 bytes long.
 */
object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = UuidProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeSerializableValue(UuidProto.serializer(), UuidProto(value.toKotlinUuid().toByteArray()))
    }

    override fun deserialize(decoder: Decoder): UUID {
        val bytes = decoder.decodeSerializableValue(UuidProto.serializer()).value
            ?: throw MissingFieldException("value", descriptor.serialName)
        if (bytes.size != Uuid.SIZE_BYTES) {
            throw SerializationException("${descriptor.serialName}.value must be ${Uuid.SIZE_BYTES} bytes, was ${bytes.size}")
        }

        return Uuid.fromByteArray(bytes).toJavaUuid()
    }
}

@Serializable
@SerialName("surf.types.v1.Uuid")
private class UuidProto(
    @ProtoNumber(1) val value: ByteArray? = null,
)
