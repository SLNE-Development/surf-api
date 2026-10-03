package dev.slne.surf.api.paper.server.nms.v26_3.glow

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.common.GlowingLifecycleHandler
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import org.bukkit.World
import org.bukkit.entity.Player

@NmsUseWithCaution
@Suppress("ClassName")
class V26_3GlowingLifecycleHandler : GlowingLifecycleHandler {
    override fun removeAllGlowingOnQuit(player: Player) {
        V26_3SurfGlowingApiImpl.removeAllGlowingOnQuit(player)
    }

    override fun getBlockGlowSpawnOperationForChunk(
        player: Player,
        chunkX: Int,
        chunkZ: Int,
        world: World
    ): PacketOperation? {
        val playerData = V26_3SurfGlowingApiImpl.getBlockPlayerData(player) ?: return null
        val blockDataList = playerData.blocks
        if (blockDataList.isEmpty()) return null

        val spawnOperation = PacketOperation.start()
        for ((loc, block) in blockDataList) {
            if (loc.chunkX != chunkX || loc.chunkZ != chunkZ || loc.world != world) continue
            spawnOperation.add(block.spawn())
        }

        return spawnOperation
    }
}
