package dev.slne.surf.api.serialization.java.number.bigdecimal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

typealias SerializableStringBigDecimal = @Serializable(
    with = BigDecimalStringSerializer::class,
) BigDecimal

/**
 * Serializes a [BigDecimal] as the string of [BigDecimal.toString], for example `"12.50"` or
 * `"1E+3"`.
 *
 * Unscaled value and scale survive a round trip, so `12.50` and `12.5` stay distinct. Decoding
 * throws [SerializationException] for a string the [BigDecimal] string constructor rejects.
 */
object BigDecimalStringSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor(
        "surfapi.java.number.BigDecimalString",
        PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val value = decoder.decodeString()

        return try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            throw SerializationException("'$value' is not a valid decimal", e)
        }
    }
}
