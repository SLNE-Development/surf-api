package dev.slne.surf.surfapi.core.api.nbt

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.nbt.CompoundBinaryTag

@InternalSurfApi
interface InternalNbtBridge {

    fun wrapCompoundBinaryTag(tag: CompoundBinaryTag, synchronize: Boolean): FastCompoundBinaryTag

    companion object {
        val instance = requiredService<InternalNbtBridge>()
    }
}