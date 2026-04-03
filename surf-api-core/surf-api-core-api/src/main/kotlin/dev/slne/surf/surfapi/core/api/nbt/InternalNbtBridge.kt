package dev.slne.surf.surfapi.core.api.nbt

import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import net.kyori.adventure.nbt.CompoundBinaryTag

private val bridge = requiredService<InternalNbtBridge>()

@InternalSurfApi
interface InternalNbtBridge {

    fun wrapCompoundBinaryTag(tag: CompoundBinaryTag, synchronize: Boolean): FastCompoundBinaryTag

    companion object : InternalNbtBridge by bridge {
        val INSTANCE get() = bridge
    }
}