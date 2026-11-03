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
