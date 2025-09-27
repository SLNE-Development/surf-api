package dev.slne.surf.surfapi.core.api.nbt

import net.kyori.adventure.nbt.*
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

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