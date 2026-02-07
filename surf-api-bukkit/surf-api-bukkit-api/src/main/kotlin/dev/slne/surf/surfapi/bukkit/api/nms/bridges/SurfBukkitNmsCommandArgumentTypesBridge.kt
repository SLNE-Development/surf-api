package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.nbt.CompoundBinaryTag

@NmsUseWithCaution
interface SurfBukkitNmsCommandArgumentTypesBridge {

    fun compoundTag(): ArgumentType<*>
    fun getCompoundTag(ctx: CommandContext<*>, key: String): CompoundBinaryTag

    companion object : SurfBukkitNmsCommandArgumentTypesBridge by commandArgumentTypes {
        val instance = commandArgumentTypes
    }
}

@NmsUseWithCaution
val commandArgumentTypes = requiredService<SurfBukkitNmsCommandArgumentTypesBridge>()