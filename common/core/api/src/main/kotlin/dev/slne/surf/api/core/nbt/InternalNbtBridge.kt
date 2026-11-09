@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.core.nbt

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.nbt.CompoundBinaryTag

private val bridge by surfBean<InternalNbtBridge>()

@InternalSurfApi
interface InternalNbtBridge {

    fun wrapCompoundBinaryTag(tag: CompoundBinaryTag, synchronize: Boolean): FastCompoundBinaryTag

    companion object {
        val INSTANCE get() = bridge
    }
}
