package dev.slne.surf.api.core.serializer.java.number.bigdecimal

import dev.slne.surf.api.core.serializer.java.number.bigint.BigIntegerSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

typealias SerializableBigDecimal = @Serializable(with = BigDecimalSerializer::class) BigDecimal


object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor =
        buildClassSerialDescriptor("surfapi.java.number.BigDecimal") {
            element("unscaledValue", BigIntegerSerializer.descriptor)
            element<Int>("scale", isOptional = true)
            element<Int>("precision", isOptional = true)
        }

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, BigIntegerSerializer, value.unscaledValue())
        encodeIntElement(descriptor, 1, value.scale())
        encodeIntElement(descriptor, 2, value.precision())
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): BigDecimal = decoder.decodeStructure(descriptor) {
        var unscaledValue: BigInteger? = null
        var scale: Int? = null
        var precision: Int? = null

        if (decodeSequentially()) {
            unscaledValue = decodeSerializableElement(descriptor, 0, BigIntegerSerializer)
            scale = decodeNullableSerializableElement(descriptor, 1, Int.serializer())
            precision = decodeNullableSerializableElement(descriptor, 2, Int.serializer())
        } else while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> unscaledValue = decodeSerializableElement(descriptor, 0, BigIntegerSerializer)
                1 -> scale = decodeIntElement(descriptor, 1)
                2 -> precision = decodeIntElement(descriptor, 2)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        requireNotNull(unscaledValue) { "Missing value for unscaledValue" }

        if (precision == null) {
            BigDecimal(unscaledValue, scale ?: 0)
        } else {
            BigDecimal(unscaledValue, scale ?: 0, MathContext(precision))
        }
    }
}