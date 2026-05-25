package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.listener

import dev.slne.surf.api.core.event.SurfEventBus
import dev.slne.surf.api.core.event.SurfEventHandler
import dev.slne.surf.api.paper.event.playtime.AfkStateChangeEvent

object PlayerAfkStateChangeEventListener {
    private var lastEvent: AfkStateChangeEvent? = null

    fun register() {
        SurfEventBus.registerListeners(this)
    }

    fun unregister() {
        SurfEventBus.unregisterListeners(this)
    }

    fun getLastEvent(): AfkStateChangeEvent? = lastEvent

    fun clearLastEvent() {
        lastEvent = null
    }

    @SurfEventHandler
    private fun onPlayerAfkStateChange(event: AfkStateChangeEvent) {
        lastEvent = event
    }
}

