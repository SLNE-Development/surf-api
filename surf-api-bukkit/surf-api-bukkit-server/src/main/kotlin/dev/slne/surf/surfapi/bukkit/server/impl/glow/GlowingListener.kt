package dev.slne.surf.surfapi.bukkit.server.impl.glow

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.util.chunkX
import dev.slne.surf.surfapi.bukkit.api.util.chunkZ
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object GlowingListener : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        SurfGlowingApiImpl.removeAllGlowingOnQuit(event.player)
    }

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        val playerData = SurfGlowingApiImpl.getBlockPlayerData(event.player) ?: return
        val blockDataList = playerData.blocks
        if (blockDataList.isEmpty()) return

        val spawnOperation = PacketOperation.start()
        for ((loc, block) in blockDataList) {
            val chunk = event.chunk
            if (loc.chunkX != chunk.x || loc.chunkZ != chunk.z || loc.world != chunk.world) continue
            spawnOperation.add(block.spawn())
        }

        spawnOperation.execute(event.player)
    }
}