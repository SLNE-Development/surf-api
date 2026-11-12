package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.math.BigDecimal

/**
 * Serializes [BigDecimal] as the protobuf message `surf.types.v1.BigDecimal`.
 *
 * Field `value` (1, required) holds [BigDecimal.toString], for example `"12.50"` or `"1E+3"`. Unscaled value and scale
 * are preserved exactly, so `12.50` and `12.5` stay distinct. The `dev.slne.surf.nats` Gradle plugin maps fields of
 * this message type to [BigDecimal] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `value` is absent and
 * [kotlinx.serialization.SerializationException] when it is not a decimal accepted by the [BigDecimal] string
 * constructor.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = BigDecimalProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeSerializableValue(BigDecimalProto.serializer(), BigDecimalProto(value.toString()))
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val value = decoder.decodeSerializableValue(BigDecimalProto.serializer()).value

        return try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            throw SerializationException("${descriptor.serialName}.value '$value' is not a valid decimal", e)
        }
    }
}

@Serializable
@SerialName("surf.types.v1.BigDecimal")
private class BigDecimalProto(
    @ProtoNumber(1) val value: String,
)
