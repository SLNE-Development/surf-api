package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.listener

import dev.slne.surf.api.event.SurfEventBus
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.paper.event.common.PlayerAfkStateChangeEvent

object PlayerAfkStateChangeEventListener {
    private var lastEvent: PlayerAfkStateChangeEvent? = null

    fun register() {
        SurfEventBus.INSTANCE.registerListeners(this)
    }

    fun unregister() {
        SurfEventBus.INSTANCE.unregisterListeners(this)
    }

    fun getLastEvent(): PlayerAfkStateChangeEvent? = lastEvent

    fun clearLastEvent() {
        lastEvent = null
    }

    @SurfEventHandler
   private fun onPlayerAfkStateChange(event: PlayerAfkStateChangeEvent) {
        lastEvent = event
    }
}

