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

/**
 * Serializes [BigInteger] as the protobuf message `surf.types.v1.BigInteger`.
 *
 * Field `value` (1, required) holds the decimal digits with an optional leading `-`, as written by
 * [BigInteger.toString]. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [BigInteger] with
 * this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `value` is absent and
 * [kotlinx.serialization.SerializationException] when it is not a decimal integer.
 */
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
