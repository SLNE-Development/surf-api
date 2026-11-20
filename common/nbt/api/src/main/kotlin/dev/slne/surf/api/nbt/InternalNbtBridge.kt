package dev.slne.surf.api.nbt

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import net.kyori.adventure.nbt.CompoundBinaryTag

@InternalSurfApi
interface InternalNbtBridge {

    fun wrapCompoundBinaryTag(tag: CompoundBinaryTag, synchronize: Boolean): FastCompoundBinaryTag

    @InternalSurfApi
    companion object {
        internal val instance by surfBean<InternalNbtBridge>()
    }
}
