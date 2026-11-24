package dev.slne.surf.api.core.server.listener.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon

object CorePacketListenerManager {
    private val listeners = setOf<PacketListenerCommon>()

    fun registerListeners() {
        for (listener in listeners) {
            register(listener)
        }
    }

    fun unregisterListeners() {
        val eventManager = PacketEvents.getAPI().eventManager
        for (listener in listeners) {
            eventManager.unregisterListener(listener)
        }
    }

    private fun register(listener: PacketListenerCommon) {
        PacketEvents.getAPI().eventManager.registerListener(listener)
    }
}
