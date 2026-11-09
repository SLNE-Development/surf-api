package dev.slne.surf.api.core.server.listener.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon

object CorePacketListenerManager {
    private val listeners = setOf<PacketListenerCommon>()

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
