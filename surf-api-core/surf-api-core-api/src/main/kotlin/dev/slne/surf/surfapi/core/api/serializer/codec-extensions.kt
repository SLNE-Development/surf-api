package dev.slne.surf.surfapi.core.api.serializer

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.PrimitiveCodec
import java.util.stream.LongStream

infix fun <F, S> Codec<F>.xor(second: Codec<S>): Codec<Either<F, S>> = Codec.xor(this, second)

fun PrimitiveCodec<LongStream>.fixedSize(size: Int): Codec<LongArray> {
    return comapFlatMap({ stream ->
        val longs = stream.limit(size + 1.toLong()).toArray()

        if (longs.size == size) {
            DataResult.success(longs)
        } else {
            val message = { "Input is not a list of $size longs" }

            if (longs.size >= size) {
                DataResult.error(message, longs.copyOf(size))
            } else {
                DataResult.error(message)
            }
        }
    }, { longs ->
        LongStream.of(*longs)
    })
}