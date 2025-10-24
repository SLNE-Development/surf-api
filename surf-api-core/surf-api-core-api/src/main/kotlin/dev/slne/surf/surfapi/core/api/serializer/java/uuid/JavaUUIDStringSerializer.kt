package dev.slne.surf.surfapi.core.api.serializer.java.uuid

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

typealias SerializableStringUUID = @Serializable(with = JavaUUIDStringSerializer::class) UUID

object JavaUUIDStringSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("surfapi.StringUuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return fromString(decoder.decodeString())
    }

    fun fromUUID(value: UUID): String {
        return value.toString().replace("-", "")
    }

    fun fromString(input: String): UUID {
        return UUID.fromString(
            input.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(), "$1-$2-$3-$4-$5"
            )
        )
    }
}