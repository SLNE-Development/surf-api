package dev.slne.surf.api.core.nbt

import net.kyori.adventure.nbt.*
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.util.Codec
import java.io.IOException
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

private val binaryTagHolderCodec = object : Codec<BinaryTag, String, IOException, IOException> {
    private val tagStringIO = TagStringIO.builder()
        .acceptLegacy(false)
        .build()

    override fun decode(encoded: String): BinaryTag {
        return tagStringIO.asTag(encoded)
    }

    override fun encode(decoded: BinaryTag): String {
        return tagStringIO.asString(decoded)
    }
}

fun BinaryTag.asTagHolder(): BinaryTagHolder {
    return BinaryTagHolder.encode(this, binaryTagHolderCodec)
}

fun BinaryTagHolder.decodeTag(): BinaryTag {
    return get(binaryTagHolderCodec)
}

fun BinaryTagHolder.decodeCompoundTag(): CompoundBinaryTag {
    val decoded = get(binaryTagHolderCodec)
    if (decoded !is CompoundBinaryTag) {
        error("Expected a CompoundBinaryTag, but got ${decoded::class.simpleName}")
    }
    return decoded
}

fun BinaryTag.isCollectionTag() =
    this is ListBinaryTag || this is ByteArrayBinaryTag || this is LongArrayBinaryTag || this is IntArrayBinaryTag

fun ListBinaryTag.tagIterator() = this.iterator()
fun ListBinaryTag.tagSpliterator() = this.spliterator()
fun ListBinaryTag.tagStream() = this.stream()

fun ByteArrayBinaryTag.tagIterator(): Iterator<BinaryTag> =
    SimpleTagIterator(::size) { ByteBinaryTag.byteBinaryTag(get(it)) }

fun ByteArrayBinaryTag.tagSpliterator() = simpleTagSpliterator(tagIterator(), size())
fun ByteArrayBinaryTag.tagStream() = simpleTagStream(tagSpliterator())


fun LongArrayBinaryTag.tagIterator(): Iterator<BinaryTag> =
    SimpleTagIterator(::size) { LongBinaryTag.longBinaryTag(get(it)) }

fun LongArrayBinaryTag.tagSpliterator() = simpleTagSpliterator(tagIterator(), size())
fun LongArrayBinaryTag.tagStream() = simpleTagStream(tagSpliterator())

fun IntArrayBinaryTag.tagIterator(): Iterator<BinaryTag> =
    SimpleTagIterator(::size) { IntBinaryTag.intBinaryTag(get(it)) }

fun IntArrayBinaryTag.tagSpliterator() = simpleTagSpliterator(tagIterator(), size())
fun IntArrayBinaryTag.tagStream() = simpleTagStream(tagSpliterator())


private class SimpleTagIterator(
    private val size: () -> Int,
    private val get: (Int) -> BinaryTag,
) : Iterator<BinaryTag> {
    private var index = 0
    override fun hasNext() = index < size()
    override fun next(): BinaryTag {
        if (!hasNext()) throw NoSuchElementException()
        return get(index++)
    }
}

private fun simpleTagSpliterator(
    iterator: Iterator<BinaryTag>,
    size: Int,
): Spliterator<BinaryTag> {
    val characteristics = Spliterator.ORDERED or
            Spliterator.SIZED or
            Spliterator.SUBSIZED or
            Spliterator.NONNULL or
            Spliterator.IMMUTABLE

    return Spliterators.spliterator(iterator, size.toLong(), characteristics)
}

private fun simpleTagStream(spliterator: Spliterator<BinaryTag>): Stream<BinaryTag> =
    StreamSupport.stream(spliterator, false)