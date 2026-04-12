package dev.slne.surf.api.paper.server.display

import com.github.retrooper.packetevents.event.PacketListenerPriority
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.paper.server.display.protocol.DisplayProtocolListener

/**
 * Handles registration and lifecycle of the display subsystem.
 */
object DisplayLoader {
    private val protocolListener = DisplayProtocolListener()

    fun onEnable() {
        packetEvents.eventManager.registerListener(
            protocolListener,
            PacketListenerPriority.LOW
        )
    }

    fun onDisable() {
        DisplayManager.closeAll()
        packetEvents.eventManager.unregisterListener(protocolListener)
    }
}
