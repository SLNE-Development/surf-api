package dev.slne.surf.surfapi.core.api.nbt

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag

interface FastCompoundBinaryTag : CompoundBinaryTag {
    fun clear()

    override fun keySet(): ObjectSet<String>

    override fun iterator(): ObjectIterator<Object2ObjectMap.Entry<String, BinaryTag>>
}

fun CompoundBinaryTag.fast(synchronize: Boolean = false) =
    InternalNbtBridge.instance.wrapCompoundBinaryTag(this, synchronize)

fun CompoundBinaryTag.Builder.buildFast(synchronize: Boolean = false) = build().fast(synchronize)