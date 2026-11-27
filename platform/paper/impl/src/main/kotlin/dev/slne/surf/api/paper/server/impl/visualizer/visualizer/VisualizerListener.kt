package dev.slne.surf.api.paper.server.impl.visualizer.visualizer

import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.server.impl.visualizer.SurfBukkitVisualizerApiImpl
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

@BukkitListener
internal class VisualizerListener(
    private val visualizers: SurfBukkitVisualizerApiImpl,
) : Listener {

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        visualizers.processChunkReceiveUpdateForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerChunkUnload(event: PlayerChunkUnloadEvent) {
        visualizers.processChunkUnloadForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        visualizers.processPlayerQuit(event.player)
    }
}
