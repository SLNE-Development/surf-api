package dev.slne.surf.surfapi.core.api.nbt

import net.kyori.adventure.nbt.*
import java.util.*
import java.util.stream.Stream

/**
 * A unified wrapper for collection-like NBT tags (ListBinaryTag, ByteArrayBinaryTag,
 * IntArrayBinaryTag, and LongArrayBinaryTag).
 *
 * This inline value class provides a common interface for iterating and accessing
 * collection-based binary tags without type checking at each usage site.
 *
 * @property tag The underlying collection binary tag
 */
@JvmInline
value class CollectionBinaryTag private constructor(val tag: BinaryTag) : Iterable<BinaryTag> {

    companion object {
        /**
         * Creates a CollectionBinaryTag from the given tag if it represents a collection type.
         *
         * @param tag The binary tag to wrap
         * @return A CollectionBinaryTag if the tag is a collection type, null otherwise
         */
        fun from(tag: BinaryTag) = if (tag.isCollectionTag()) CollectionBinaryTag(tag) else null

        /**
         * Creates a CollectionBinaryTag from the given tag, throwing if it's not a collection type.
         *
         * @param tag The binary tag to wrap
         * @return A CollectionBinaryTag wrapping the tag
         * @throws IllegalArgumentException if the tag is not a collection type
         */
        fun require(tag: BinaryTag): CollectionBinaryTag {
            require(tag.isCollectionTag()) { "Tag is not a collection tag" }
            return CollectionBinaryTag(tag)
        }
    }

    /**
     * Returns an iterator over the elements in this collection tag.
     */
    override fun iterator() = when (tag) {
        is ListBinaryTag -> tag.tagIterator()
        is ByteArrayBinaryTag -> tag.tagIterator()
        is LongArrayBinaryTag -> tag.tagIterator()
        is IntArrayBinaryTag -> tag.tagIterator()
        else -> throw MatchException(null, null)
    }

    /**
     * Returns a spliterator over the elements in this collection tag.
     */
    fun tagSpliterator(): Spliterator<BinaryTag> = when (tag) {
        is ListBinaryTag -> tag.tagSpliterator()
        is ByteArrayBinaryTag -> tag.tagSpliterator()
        is LongArrayBinaryTag -> tag.tagSpliterator()
        is IntArrayBinaryTag -> tag.tagSpliterator()
        else -> throw MatchException(null, null)
    }

    /**
     * Returns the number of elements in this collection tag.
     */
    fun size(): Int = when (tag) {
        is ListBinaryTag -> tag.size()
        is ByteArrayBinaryTag -> tag.size()
        is LongArrayBinaryTag -> tag.size()
        is IntArrayBinaryTag -> tag.size()
        else -> throw MatchException(null, null)
    }

    /**
     * Returns a sequential stream over the elements in this collection tag.
     */
    fun stream(): Stream<BinaryTag> = when (tag) {
        is ListBinaryTag -> tag.tagStream()
        is ByteArrayBinaryTag -> tag.tagStream()
        is LongArrayBinaryTag -> tag.tagStream()
        is IntArrayBinaryTag -> tag.tagStream()
        else -> throw MatchException(null, null)
    }
}
/**
 * Attempts to wrap this BinaryTag as a CollectionBinaryTag.
 *
 * @return A CollectionBinaryTag if this tag is a collection type, null otherwise
 */
fun BinaryTag.asCollectionOrNull() = CollectionBinaryTag.from(this)

/**
 * Wraps this BinaryTag as a CollectionBinaryTag.
 *
 * @return A CollectionBinaryTag wrapping this tag
 * @throws IllegalArgumentException if this tag is not a collection type
 */
fun BinaryTag.asCollection() = CollectionBinaryTag.require(this)