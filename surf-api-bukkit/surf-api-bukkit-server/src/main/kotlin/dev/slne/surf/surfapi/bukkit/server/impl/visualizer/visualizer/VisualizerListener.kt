package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object VisualizerListener : Listener {

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        VisualizerManager.processChunkReceiveUpdateForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerChunkUnload(event: PlayerChunkUnloadEvent) {
        VisualizerManager.processChunkUnloadForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        VisualizerManager.processPlayerQuit(event.player)
    }
}