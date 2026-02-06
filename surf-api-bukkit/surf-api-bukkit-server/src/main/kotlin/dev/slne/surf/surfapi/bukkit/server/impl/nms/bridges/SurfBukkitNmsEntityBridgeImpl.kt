package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges

import ca.spottedleaf.moonrise.common.util.TickThread
import com.google.auto.service.AutoService
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsEntityBridge
import dev.slne.surf.surfapi.bukkit.api.util.chunkX
import dev.slne.surf.surfapi.bukkit.api.util.chunkZ
import dev.slne.surf.surfapi.bukkit.server.nms.AdventureNBT
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.bukkit.server.nms.toNmsHolder
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.commands.SummonCommand
import org.bukkit.World
import org.bukkit.entity.EntityType

@NmsUseWithCaution
@AutoService(SurfBukkitNmsEntityBridge::class)
class SurfBukkitNmsEntityBridgeImpl : SurfBukkitNmsEntityBridge {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun createEntityByNbt(
        world: World,
        type: EntityType,
        pos: FinePosition,
        tag: CompoundBinaryTag
    ) {
        val worldNMS = world.toNms()
        TickThread.ensureTickThread(worldNMS, pos.chunkX, pos.chunkZ, "Cannot create entity asynchronously")

        val source = MinecraftServer.getServer().createCommandSourceStack()
            .withLevel(worldNMS)

        try {
            SummonCommand.createEntity(source, type.toNmsHolder(), pos.toNms(), AdventureNBT.toNms(tag), false)
        } catch (e: CommandSyntaxException) {
            throw WrapperCommandSyntaxException(e)
        }
    }
}