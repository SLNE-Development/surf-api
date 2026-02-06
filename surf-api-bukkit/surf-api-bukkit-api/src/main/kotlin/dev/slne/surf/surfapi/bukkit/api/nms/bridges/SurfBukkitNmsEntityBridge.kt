package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.bukkit.World
import org.bukkit.entity.EntityType

@NmsUseWithCaution
interface SurfBukkitNmsEntityBridge {

    @Throws(WrapperCommandSyntaxException::class)
    fun createEntityByNbt(world: World, type: EntityType, pos: FinePosition, tag: CompoundBinaryTag)

    companion object : SurfBukkitNmsEntityBridge by entityBridge {
        val instance = entityBridge
    }
}

@NmsUseWithCaution
val entityBridge = requiredService<SurfBukkitNmsEntityBridge>()