package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizerApiImpl
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object VisualizerListener : Listener {

    @EventHandler
    fun onPlayerChunkLoad(event: PlayerChunkLoadEvent) {
        visualizerApiImpl.processChunkReceiveUpdateForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerChunkUnload(event: PlayerChunkUnloadEvent) {
        visualizerApiImpl.processChunkUnloadForPlayer(event.player, event.chunk)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        visualizerApiImpl.processPlayerQuit(event.player)
    }
}