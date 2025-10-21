package dev.slne.surf.surfapi.core.api.serializer

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.PrimitiveCodec
import java.util.stream.LongStream
import com.mojang.datafixers.util.Pair as DataFixerPair

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

fun <A> Codec<A>.ranged(
    minInclusive: A,
    maxInclusive: A,
): Codec<A> where A : Number, A : Comparable<A> = validate { number ->
    when {
        number < minInclusive -> DataResult.error { "Number is too small: $number, expected range [$minInclusive-$maxInclusive]" }
        number > maxInclusive -> DataResult.error { "Number is too big: $number, expected range [$minInclusive-$maxInclusive]" }
        else -> DataResult.success(number)
    }
}

fun <A> Codec<A>.positive(
    zeroAllowed: Boolean = true,
    zero: A,
    compare: (A, A) -> Int,
): Codec<A> = validate { number ->
    val cmp = compare(number, zero)
    when {
        cmp < 0 -> DataResult.error { "Number is negative: $number, expected positive" }
        !zeroAllowed && cmp == 0 -> DataResult.error { "Number is zero: $number, expected positive" }
        else -> DataResult.success(number)
    }
}

@JvmName("positiveLong")
fun Codec<Long>.positive(zeroAllowed: Boolean = true) = positive(zeroAllowed, 0L, Long::compareTo)

@JvmName("positiveInt")
fun Codec<Int>.positive(zeroAllowed: Boolean = true) = positive(zeroAllowed, 0, Int::compareTo)

@JvmName("positiveDouble")
fun Codec<Double>.positive(zeroAllowed: Boolean = true) =
    positive(zeroAllowed, 0.0, Double::compareTo)

@JvmName("positiveFloat")
fun Codec<Float>.positive(zeroAllowed: Boolean = true) = positive(zeroAllowed, 0f, Float::compareTo)

@JvmName("positiveShort")
fun Codec<Short>.positive(zeroAllowed: Boolean = true) =
    positive(zeroAllowed, 0.toShort(), Short::compareTo)

@JvmName("positiveByte")
fun Codec<Byte>.positive(zeroAllowed: Boolean = true) =
    positive(zeroAllowed, 0.toByte(), Byte::compareTo)

operator fun <F, S> DataFixerPair<F, S>.component1(): F = first
operator fun <F, S> DataFixerPair<F, S>.component2(): S = second
infix fun <F, S> F.toDataFixerPair(other: S): DataFixerPair<F, S> = DataFixerPair(this, other)