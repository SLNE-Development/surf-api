package dev.slne.surf.api.core.server.listener

import dev.slne.surf.api.core.server.listener.packet.CorePacketListenerManager

object CoreListenerManager {
    fun registerListeners() {
        CorePacketListenerManager.registerListeners()
    }

    fun unregisterListeners() {
        CorePacketListenerManager.unregisterListeners()
    }
}
