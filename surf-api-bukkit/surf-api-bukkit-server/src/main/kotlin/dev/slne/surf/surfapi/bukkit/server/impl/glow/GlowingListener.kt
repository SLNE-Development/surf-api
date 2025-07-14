package dev.slne.surf.surfapi.bukkit.server.impl.glow

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object GlowingListener : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        SurfGlowingApiImpl.removeAllGlowingOnQuit(event.player)
    }

}