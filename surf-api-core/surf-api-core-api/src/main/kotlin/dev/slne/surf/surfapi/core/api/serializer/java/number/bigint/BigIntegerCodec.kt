package dev.slne.surf.surfapi.core.api.serializer.java.number.bigint

import com.mojang.serialization.Codec
import java.math.BigInteger

object BigIntegerCodec {
    val CODEC: Codec<BigInteger> = Codec.STRING.xmap(::BigInteger, BigInteger::toString)
}