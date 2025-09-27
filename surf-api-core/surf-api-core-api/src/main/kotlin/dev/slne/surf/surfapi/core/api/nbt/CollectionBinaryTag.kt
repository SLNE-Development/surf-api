package dev.slne.surf.surfapi.core.api.nbt

import net.kyori.adventure.nbt.*
import java.lang.Iterable
import java.util.*
import java.util.stream.Stream

@JvmInline
value class CollectionBinaryTag private constructor(val tag: BinaryTag) : Iterable<BinaryTag> {

    companion object {
        fun from(tag: BinaryTag) = if (tag.isCollectionTag()) CollectionBinaryTag(tag) else null
        fun require(tag: BinaryTag): CollectionBinaryTag {
            require(tag.isCollectionTag()) { "Tag is not a collection tag" }
            return CollectionBinaryTag(tag)
        }
    }

    override fun iterator() = when (tag) {
        is ListBinaryTag -> tag.tagIterator()
        is ByteArrayBinaryTag -> tag.tagIterator()
        is LongArrayBinaryTag -> tag.tagIterator()
        is IntArrayBinaryTag -> tag.tagIterator()
        else -> throw MatchException(null, null)
    }

    fun tagSpliterator(): Spliterator<BinaryTag> = when (tag) {
        is ListBinaryTag -> tag.tagSpliterator()
        is ByteArrayBinaryTag -> tag.tagSpliterator()
        is LongArrayBinaryTag -> tag.tagSpliterator()
        is IntArrayBinaryTag -> tag.tagSpliterator()
        else -> throw MatchException(null, null)
    }

    fun size(): Int = when (tag) {
        is ListBinaryTag -> tag.size()
        is ByteArrayBinaryTag -> tag.size()
        is LongArrayBinaryTag -> tag.size()
        is IntArrayBinaryTag -> tag.size()
        else -> throw MatchException(null, null)
    }

    fun stream(): Stream<BinaryTag> = when (tag) {
        is ListBinaryTag -> tag.tagStream()
        is ByteArrayBinaryTag -> tag.tagStream()
        is LongArrayBinaryTag -> tag.tagStream()
        is IntArrayBinaryTag -> tag.tagStream()
        else -> throw MatchException(null, null)
    }
}

fun BinaryTag.asCollectionOrNull() = CollectionBinaryTag.from(this)
fun BinaryTag.asCollection() = CollectionBinaryTag.require(this)