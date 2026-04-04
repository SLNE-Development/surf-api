package dev.slne.surf.api.paper.server.nms

import net.kyori.adventure.nbt.*
import net.minecraft.nbt.*

object AdventureNBT {
    fun toNms(
        adventure: CompoundBinaryTag,
        accounter: NbtAccounter = NbtAccounter.unlimitedHeap()
    ): CompoundTag {
        return CompoundTag().also { tag ->
            for ((key, value) in adventure) {
                tag.put(key, toNms(value, accounter))
            }
        }
    }

    fun toNms(
        adventure: BinaryTag,
        accounter: NbtAccounter = NbtAccounter.unlimitedHeap()
    ): Tag {
        accounter.pushDepth()

        val nmsTag = when (adventure) {
            is IntBinaryTag -> IntTag.valueOf(adventure.value())
            is ByteBinaryTag -> ByteTag.valueOf(adventure.value())
            is FloatBinaryTag -> FloatTag.valueOf(adventure.value())
            is LongBinaryTag -> LongTag.valueOf(adventure.value())
            is DoubleBinaryTag -> DoubleTag.valueOf(adventure.value())
            is ShortBinaryTag -> ShortTag.valueOf(adventure.value())
            is StringBinaryTag -> StringTag.valueOf(adventure.value())
            is ByteArrayBinaryTag -> ByteArrayTag(adventure.value())
            is IntArrayBinaryTag -> IntArrayTag(adventure.value())
            is LongArrayBinaryTag -> LongArrayTag(adventure.value())
            is EndBinaryTag -> EndTag.INSTANCE
            is ListBinaryTag -> if (adventure.isEmpty) {
                ListTag()
            } else {
                val list = ListTag()
                for (entry in adventure) {
                    val nms = toNms(entry)
                    list.add(nms)
                }
                list
            }

            is CompoundBinaryTag -> {
                val tag = CompoundTag()
                for ((key, entry) in adventure) {
                    val nms = toNms(entry)
                    tag.put(key, nms)
                }
                tag
            }

            else -> throw IllegalArgumentException("Unsupported tag type: ${adventure::class}")
        }

        accounter.popDepth()

        return nmsTag
    }

    fun fromNms(nms: CompoundTag): CompoundBinaryTag = CompoundBinaryTag.builder().also { builder ->
        nms.forEach { key, tag -> builder.put(key, fromNms(tag)) }
    }.build()

    fun fromNms(nms: Tag): BinaryTag = when (nms) {
        is IntTag -> IntBinaryTag.intBinaryTag(nms.intValue())
        is ByteTag -> ByteBinaryTag.byteBinaryTag(nms.byteValue())
        is FloatTag -> FloatBinaryTag.floatBinaryTag(nms.floatValue())
        is LongTag -> LongBinaryTag.longBinaryTag(nms.longValue())
        is DoubleTag -> DoubleBinaryTag.doubleBinaryTag(nms.doubleValue())
        is ShortTag -> ShortBinaryTag.shortBinaryTag(nms.shortValue())
        is StringTag -> StringBinaryTag.stringBinaryTag(nms.value())
        is ByteArrayTag -> ByteArrayBinaryTag.byteArrayBinaryTag(*nms.asByteArray)
        is IntArrayTag -> IntArrayBinaryTag.intArrayBinaryTag(*nms.asIntArray)
        is LongArrayTag -> LongArrayBinaryTag.longArrayBinaryTag(*nms.asLongArray)
        is EndTag -> EndBinaryTag.endBinaryTag()
        is ListTag -> {
            val tag = ListBinaryTag.heterogeneousListBinaryTag()
            for (t in nms) {
                tag.add(fromNms(t))
            }
            tag.build()
        }

        is CompoundTag -> {
            val adventure = CompoundBinaryTag.builder()
            nms.forEach { key, tag ->
                adventure.put(key, fromNms(tag))
            }
            adventure.build()
        }
    }
}