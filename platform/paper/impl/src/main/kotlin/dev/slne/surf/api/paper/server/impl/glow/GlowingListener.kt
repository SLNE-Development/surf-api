package dev.slne.surf.api.paper.server.impl.glow

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.GlowingLifecycleHandler
import dev.slne.surf.api.paper.nms.common.NmsProvider
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

@OptIn(NmsUseWithCaution::class)
object GlowingListener : Listener {
    private val glowingLifecycleHandler: GlowingLifecycleHandler by lazy {
        NmsProvider.current.createGlowingLifecycleHandler()
    }

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
            chunk.world
        ) ?: return

        operation.execute(event.player)
    }
}