package dev.slne.surf.surfapi.core.server.listener

import dev.slne.surf.surfapi.core.server.listener.packet.CorePacketListenerManager

object CoreListenerManager {
    fun registerListeners() {
        CorePacketListenerManager.registerListeners()
    }

    fun unregisterListeners() {
        CorePacketListenerManager.unregisterListeners()
    }
}
