package dev.slne.surf.api.paper.nms.bridges

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import net.kyori.adventure.nbt.CompoundBinaryTag

@NmsUseWithCaution
interface SurfPaperNmsCommandArgumentTypesBridge {

    fun compoundTag(): ArgumentType<*>
    fun getCompoundTag(ctx: CommandContext<*>, key: String): CompoundBinaryTag

    companion object : SurfPaperNmsCommandArgumentTypesBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsCommandArgumentTypesBridge>()