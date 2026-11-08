package dev.slne.surf.api.core.server.impl.nbt

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.nbt.FastCompoundBinaryTag
import dev.slne.surf.api.core.nbt.InternalNbtBridge
import net.kyori.adventure.nbt.CompoundBinaryTag

@AutoService(InternalNbtBridge::class)
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