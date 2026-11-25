package dev.slne.surf.api.nbt

import net.kyori.adventure.nbt.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * Backs [FastCompoundBinaryTag] with a [ConcurrentHashMap] when [concurrent] is set and with a
 * [HashMap] otherwise.
 *
 * @param concurrent whether the tag may be read and written by several threads at once
 */
internal class FastCompoundBinaryTagImpl(concurrent: Boolean) : FastCompoundBinaryTag {

    private val tags: MutableMap<String, BinaryTag> =
        if (concurrent) ConcurrentHashMap() else HashMap()

    override fun contains(key: String): Boolean {
        return tags.containsKey(key)
    }

    override fun contains(key: String, type: BinaryTagType<*>): Boolean {
        val tag = tags[key] ?: return false

        return type.test(tag.type())
    }

    override fun keySet(): Set<String> = Collections.unmodifiableSet(tags.keys)
    override fun get(key: String): BinaryTag? = tags[key]
    override fun size(): Int = tags.size
    override fun isEmpty(): Boolean = tags.isEmpty()

    override fun put(key: String, tag: BinaryTag): FastCompoundBinaryTagImpl {
        return apply {
            tags[key] = tag
        }
    }

    override fun put(tag: CompoundBinaryTag): FastCompoundBinaryTagImpl {
        return apply {
            for (key in tag.keySet()) {
                tags[key] = tag.get(key) ?: continue
            }
        }
    }

    override fun put(tags: Map<String, BinaryTag>): FastCompoundBinaryTagImpl {
        return apply {
            this.tags.putAll(tags)
        }
    }

    override fun remove(key: String, removed: Consumer<in BinaryTag>?): FastCompoundBinaryTagImpl {
        return apply {
            val removedTag = tags.remove(key)

            if (removed != null && removedTag != null) {
                removed.accept(removedTag)
            }
        }
    }

    override fun clear() {
        tags.clear()
    }

    override fun getByte(key: String, defaultValue: Byte): Byte {
        val tag = tags[key] as? NumberBinaryTag ?: return defaultValue

        return tag.byteValue()
    }

    override fun getShort(key: String, defaultValue: Short): Short {
        val tag = tags[key] as? NumberBinaryTag ?: return defaultValue

        return tag.shortValue()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        val tag = tags[key] as? NumberBinaryTag ?: return defaultValue

        return tag.intValue()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val tag = tags[key] as? NumberBinaryTag ?: return defaultValue

        return tag.longValue()
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        val tag = tags[key] as? NumberBinaryTag ?: return defaultValue

        return tag.floatValue()
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        val tag = tags[key] as? NumberBinaryTag ?: return defaultValue

        return tag.doubleValue()
    }

    override fun getByteArray(key: String): ByteArray {
        return getByteArray(key, ByteArray(0))
            ?: ByteArray(0)
    }

    override fun getByteArray(key: String, defaultValue: ByteArray?): ByteArray? {
        val tag = tags[key] as? ByteArrayBinaryTag ?: return defaultValue

        return tag.value()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        val tag = tags[key] as? StringBinaryTag ?: return defaultValue

        return tag.value()
    }

    override fun getList(key: String, defaultValue: ListBinaryTag?): ListBinaryTag? {
        val tag = tags[key] as? ListBinaryTag ?: return defaultValue

        return tag
    }

    override fun getList(
        key: String,
        expectedType: BinaryTagType<out BinaryTag>,
        defaultValue: ListBinaryTag?,
    ): ListBinaryTag? {
        val tag = tags[key] as? ListBinaryTag ?: return defaultValue

        return if (expectedType.test(tag.type())) tag else defaultValue
    }

    override fun getCompound(key: String, defaultValue: CompoundBinaryTag?): CompoundBinaryTag? {
        val tag = tags[key] as? CompoundBinaryTag ?: return defaultValue

        return tag
    }

    override fun getIntArray(key: String): IntArray {
        return getIntArray(key, IntArray(0))
            ?: IntArray(0)
    }

    override fun getIntArray(key: String, defaultValue: IntArray?): IntArray? {
        val tag = tags[key] as? IntArrayBinaryTag ?: return defaultValue

        return tag.value()
    }

    override fun getLongArray(key: String): LongArray {
        return getLongArray(key, LongArray(0))
            ?: LongArray(0)
    }

    override fun getLongArray(key: String, defaultValue: LongArray?): LongArray? {
        val tag = tags[key] as? LongArrayBinaryTag ?: return defaultValue

        return tag.value()
    }

    override fun stream(): Stream<Map.Entry<String, BinaryTag>> {
        val entries: Set<Map.Entry<String, BinaryTag>> = tags.entries

        return entries.stream()
    }

    override fun iterator(): Iterator<Map.Entry<String, BinaryTag>> = tags.entries.iterator()

    override fun forEach(action: Consumer<in MutableMap.MutableEntry<String, out BinaryTag>>) {
        tags.entries.forEach(action)
    }

    override fun asBinaryTag(): CompoundBinaryTag {
        return CompoundBinaryTag.from(tags)
    }

    override fun toString(): String = "FastCompoundBinaryTag{tags=$tags}"
}
