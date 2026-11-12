package dev.slne.surf.api.core.server.impl.nbt

import dev.slne.surf.api.nbt.FastCompoundBinaryTag
import dev.slne.surf.api.nbt.InternalNbtBridge
import dev.slne.surf.api.di.Service
import net.kyori.adventure.nbt.CompoundBinaryTag

@Service
class InternalNbtBridgeImpl : InternalNbtBridge {
    override fun wrapCompoundBinaryTag(
        tag: CompoundBinaryTag,
        synchronize: Boolean,
    ): FastCompoundBinaryTag {
        val fastTag = FastCompoundBinaryTagImpl(synchronize)
        tag.forEach { (key, tag) ->
            fastTag.put(key, tag)
        }
        return fastTag
    }
}