package dev.slne.surf.surfapi.bukkit.server.nms

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
}