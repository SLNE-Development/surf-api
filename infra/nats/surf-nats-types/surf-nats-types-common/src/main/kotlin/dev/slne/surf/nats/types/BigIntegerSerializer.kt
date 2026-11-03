package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.math.BigInteger

object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor = BigIntegerProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: BigInteger) {
        encoder.encodeSerializableValue(BigIntegerProto.serializer(), BigIntegerProto(value.toString()))
    }

    override fun deserialize(decoder: Decoder): BigInteger {
        val value = decoder.decodeSerializableValue(BigIntegerProto.serializer()).value

        return try {
            BigInteger(value)
        } catch (e: NumberFormatException) {
            throw SerializationException("${descriptor.serialName}.value '$value' is not a valid integer", e)
        }
    }
}

@Serializable
@SerialName("surf.types.v1.BigInteger")
private class BigIntegerProto(
    @ProtoNumber(1) val value: String,
)
