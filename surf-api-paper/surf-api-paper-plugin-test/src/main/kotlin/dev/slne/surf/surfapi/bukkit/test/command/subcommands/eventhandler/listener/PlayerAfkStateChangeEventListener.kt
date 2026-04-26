package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.listener

import dev.slne.surf.api.core.event.SurfEventBus
import dev.slne.surf.api.core.event.SurfEventHandler
import dev.slne.surf.api.paper.event.common.PlayerAfkStateChangeEvent

object PlayerAfkStateChangeEventListener {
    private var lastEvent: PlayerAfkStateChangeEvent? = null

    fun register() {
        SurfEventBus.registerListeners(this)
    }

    fun unregister() {
        SurfEventBus.unregisterListeners(this)
    }

    fun getLastEvent(): PlayerAfkStateChangeEvent? = lastEvent

    fun clearLastEvent() {
        lastEvent = null
    }

    @SurfEventHandler
    fun onPlayerAfkStateChange(event: PlayerAfkStateChangeEvent) {
        lastEvent = event
    }
}

