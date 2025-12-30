package dev.slne.surf.surfapi.core.api.serializer.java.number.bigdecimal

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.java.number.bigint.BigIntegerCodec
import java.math.BigDecimal

object BigDecimalCodec {
    val CODEC: Codec<BigDecimal> = RecordCodecBuilder.create { instance ->
        instance.group(
            BigIntegerCodec.CODEC.fieldOf("unscaledValue").forGetter(BigDecimal::unscaledValue),
            Codec.INT.optionalFieldOf("scale", 0).forGetter(BigDecimal::scale),
            Codec.INT.optionalFieldOf("precision", null).forGetter(BigDecimal::precision),
        ).apply(instance) { unscaledValue, scale, precision ->
            if (precision == null) {
                BigDecimal(unscaledValue, scale)
            } else {
                BigDecimal(unscaledValue, scale, java.math.MathContext(precision))
            }
        }
    }
}