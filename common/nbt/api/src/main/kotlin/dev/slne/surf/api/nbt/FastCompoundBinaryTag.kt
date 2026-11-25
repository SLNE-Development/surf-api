package dev.slne.surf.api.nbt

import net.kyori.adventure.nbt.*
import org.jetbrains.annotations.UnmodifiableView
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * A mutable, high-performance replacement for CompoundBinaryTag.
 *
 * This interface mirrors the CompoundBinaryTag API but cannot extend it since CompoundBinaryTag
 * became sealed in Adventure 5.1.1. All operations (put, remove, etc.) mutate the tag directly,
 * unlike the standard CompoundBinaryTag which is immutable.
 *
 * A tag from `fast()` or `buildFast()` is not thread-safe unless it was created with
 * `concurrent = true`. A concurrent tag allows concurrent reads and writes; its iteration,
 * [stream] and [keySet] are weakly consistent views, and [asBinaryTag] takes a snapshot that may
 * miss concurrent writes.
 */
interface FastCompoundBinaryTag : BinaryTagLike {

    fun type(): BinaryTagType<CompoundBinaryTag> = BinaryTagTypes.COMPOUND

    fun contains(key: String): Boolean
    fun contains(key: String, type: BinaryTagType<*>): Boolean

    fun keySet(): @UnmodifiableView Set<String>

    fun get(key: String): BinaryTag?
    fun size(): Int
    fun isEmpty(): Boolean

    fun put(key: String, tag: BinaryTag): FastCompoundBinaryTag
    fun put(tag: CompoundBinaryTag): FastCompoundBinaryTag
    fun put(tags: Map<String, BinaryTag>): FastCompoundBinaryTag
    fun remove(key: String, removed: Consumer<in BinaryTag>? = null): FastCompoundBinaryTag

    fun clear()

    fun getByte(key: String, defaultValue: Byte = 0): Byte
    fun getShort(key: String, defaultValue: Short = 0): Short
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun getFloat(key: String, defaultValue: Float = 0f): Float
    fun getDouble(key: String, defaultValue: Double = 0.0): Double
    fun getByteArray(key: String): ByteArray
    fun getByteArray(key: String, defaultValue: ByteArray?): ByteArray?
    fun getString(key: String, defaultValue: String? = null): String?
    fun getList(key: String, defaultValue: ListBinaryTag? = null): ListBinaryTag?
    fun getList(
        key: String,
        expectedType: BinaryTagType<out BinaryTag>,
        defaultValue: ListBinaryTag? = null,
    ): ListBinaryTag?

    fun getCompound(key: String, defaultValue: CompoundBinaryTag? = null): CompoundBinaryTag?
    fun getIntArray(key: String): IntArray
    fun getIntArray(key: String, defaultValue: IntArray?): IntArray?
    fun getLongArray(key: String): LongArray
    fun getLongArray(key: String, defaultValue: LongArray?): LongArray?

    fun stream(): Stream<Map.Entry<String, BinaryTag>>
    fun iterator(): Iterator<Map.Entry<String, BinaryTag>>
    fun forEach(action: Consumer<in MutableMap.MutableEntry<String, out BinaryTag>>)

    override fun asBinaryTag(): CompoundBinaryTag
}

/**
 * Copies the entries of this tag into a new mutable [FastCompoundBinaryTag].
 *
 * @param concurrent whether the copy may be read and written by several threads at once
 * @return a mutable copy of this tag, independent of the original
 */
fun CompoundBinaryTag.fast(concurrent: Boolean = false): FastCompoundBinaryTag {
    return FastCompoundBinaryTagImpl(concurrent).put(this)
}

/**
 * Builds the tag and copies its entries into a new mutable [FastCompoundBinaryTag].
 *
 * @param concurrent whether the copy may be read and written by several threads at once
 * @return a mutable copy of the built tag
 */
fun CompoundBinaryTag.Builder.buildFast(concurrent: Boolean = false): FastCompoundBinaryTag {
    return build().fast(concurrent)
}
