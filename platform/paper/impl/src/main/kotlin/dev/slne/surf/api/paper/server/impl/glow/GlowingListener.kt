package dev.slne.surf.api.paper.server.impl.glow

import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.nms.common.GlowingLifecycleHandler
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

@BukkitListener
internal class GlowingListener(
    private val glowingLifecycleHandler: GlowingLifecycleHandler,
) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        glowingLifecycleHandler.removeAllGlowingOnQuit(event.player)
    }

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        val chunk = event.chunk
        val operation = glowingLifecycleHandler.getBlockGlowSpawnOperationForChunk(
            event.player,
            chunk.x,
            chunk.z,
            chunk.world,
        ) ?: return

        operation.execute(event.player)
    }
}
