package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import ca.spottedleaf.moonrise.common.util.TickThread
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsEntityBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.AdventureNBT
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNmsHolder
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.commands.SummonCommand
import org.bukkit.World
import org.bukkit.entity.EntityType

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsEntityBridgeImpl : SurfPaperNmsEntityBridge {

    @Suppress("UnstableApiUsage")
    override fun createEntityByNbt(
        world: World,
        type: EntityType,
        pos: FinePosition,
        tag: CompoundBinaryTag
    ) {
        val worldNMS = world.toNms()
        TickThread.ensureTickThread(
            worldNMS,
            pos.chunkX,
            pos.chunkZ,
            "Cannot create entity asynchronously"
        )

        val source = MinecraftServer.getServer().createCommandSourceStack()
            .withLevel(worldNMS)

        try {
            SummonCommand.createEntity(
                source,
                type.toNmsHolder(),
                pos.toNms(),
                AdventureNBT.toNms(tag),
                false
            )
        } catch (e: CommandSyntaxException) {
            throw WrapperCommandSyntaxException(e)
        }
    }
}
