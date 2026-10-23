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
