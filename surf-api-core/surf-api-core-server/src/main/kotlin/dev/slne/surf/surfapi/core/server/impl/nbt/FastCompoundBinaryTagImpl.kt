package dev.slne.surf.surfapi.core.server.impl.nbt

import dev.slne.surf.surfapi.core.api.nbt.FastCompoundBinaryTag
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.synchronize
import net.kyori.adventure.nbt.*
import net.kyori.examination.ExaminableProperty
import net.kyori.examination.string.StringExaminer
import java.util.function.Consumer
import java.util.stream.Stream

class FastCompoundBinaryTagImpl(synchronize: Boolean) : FastCompoundBinaryTag {

    private val tags =
        mutableObject2ObjectMapOf<String, BinaryTag>().let { if (synchronize) it.synchronize() else it }

    fun contains(key: String, type: BinaryTagType<*>): Boolean {
        val tag = tags[key]
        return tag != null && type.test(tag.type())
    }

    override fun keySet() = tags.keys.freeze()
    override fun get(key: String) = tags[key]
    override fun size() = tags.size
    override fun isEmpty() = tags.isEmpty()

    override fun put(key: String, tag: BinaryTag) = apply {
        tags[key] = tag
    }

    override fun put(tag: CompoundBinaryTag) = apply {
        for (key in tag.keySet()) {
            tags[key] = tag.get(key) ?: continue
        }
    }

    override fun put(tags: Map<String, BinaryTag>) = apply {
        this.tags.putAll(tags)
    }

    override fun remove(key: String, removed: Consumer<in BinaryTag>?) = apply {
        val removedTag = tags.remove(key)
        if (removed != null && removedTag != null) {
            removed.accept(removedTag)
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

    override fun getByteArray(key: String) = getByteArray(key, ByteArray(0))
    override fun getByteArray(key: String, defaultValue: ByteArray): ByteArray {
        val tag = tags[key] as? ByteArrayBinaryTag ?: return defaultValue
        return tag.value()
    }

    override fun getString(key: String, defaultValue: String): String {
        val tag = tags[key] as? StringBinaryTag ?: return defaultValue
        return tag.value()
    }

    override fun getList(key: String, defaultValue: ListBinaryTag): ListBinaryTag {
        val tag = tags[key] as? ListBinaryTag ?: return defaultValue
        return tag
    }

    override fun getList(
        key: String,
        expectedType: BinaryTagType<out BinaryTag>,
        defaultValue: ListBinaryTag
    ): ListBinaryTag {
        val tag = tags[key] as? ListBinaryTag ?: return defaultValue
        return if (expectedType.test(tag.type())) tag else defaultValue
    }

    override fun getCompound(key: String, defaultValue: CompoundBinaryTag): CompoundBinaryTag {
        val tag = tags[key] as? CompoundBinaryTag ?: return defaultValue
        return tag
    }

    override fun getIntArray(key: String) = getIntArray(key, IntArray(0))
    override fun getIntArray(key: String, defaultValue: IntArray): IntArray {
        val tag = tags[key] as? IntArrayBinaryTag ?: return defaultValue
        return tag.value()
    }

    override fun getLongArray(key: String) = getLongArray(key, LongArray(0))
    override fun getLongArray(key: String, defaultValue: LongArray): LongArray {
        val tag = tags[key] as? LongArrayBinaryTag ?: return defaultValue
        return tag.value()
    }

    override fun stream(): Stream<Map.Entry<String, BinaryTag>> = tags.entries.stream()

    override fun examinableProperties(): Stream<ExaminableProperty> = Stream.of(
        ExaminableProperty.of("tags", tags)
    )

    override fun iterator() = tags.object2ObjectEntrySet().iterator()

    override fun forEach(action: Consumer<in MutableMap.MutableEntry<String, out BinaryTag>>) {
        tags.object2ObjectEntrySet().forEach(action)
    }

    override fun examinableName(): String {
        return type().toString()
    }

    override fun toString(): String {
        return examine(StringExaminer.simpleEscaping())
    }
}