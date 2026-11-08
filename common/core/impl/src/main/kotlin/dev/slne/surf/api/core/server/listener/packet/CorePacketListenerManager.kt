package dev.slne.surf.api.core.server.listener.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon
import dev.slne.surf.api.core.util.objectSetOf

object CorePacketListenerManager {
    private val listeners = objectSetOf<PacketListenerCommon>()

    fun registerListeners() {
        listeners.forEach { register(it) }
    }

    fun unregisterListeners() {
        val eventManager = PacketEvents.getAPI().eventManager
        listeners.forEach { eventManager.unregisterListener(it) }
    }

    private fun register(listener: PacketListenerCommon) {
        PacketEvents.getAPI().eventManager.registerListener(listener)
    }
}
