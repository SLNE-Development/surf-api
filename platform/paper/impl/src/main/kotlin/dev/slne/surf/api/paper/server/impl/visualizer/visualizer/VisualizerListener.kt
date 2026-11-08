package dev.slne.surf.api.paper.server.impl.visualizer.visualizer

import dev.slne.surf.api.paper.server.impl.visualizer.SurfBukkitVisualizerApiImpl
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object VisualizerListener : Listener {

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        SurfBukkitVisualizerApiImpl.INSTANCE.processChunkReceiveUpdateForPlayer(
            event.player,
            event.chunk
        )
    }

    @EventHandler
    fun onPlayerChunkUnload(event: PlayerChunkUnloadEvent) {
        SurfBukkitVisualizerApiImpl.INSTANCE.processChunkUnloadForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        SurfBukkitVisualizerApiImpl.INSTANCE.processPlayerQuit(event.player)
    }
}