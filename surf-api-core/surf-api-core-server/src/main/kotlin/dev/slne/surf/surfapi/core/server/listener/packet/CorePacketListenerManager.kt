package dev.slne.surf.surfapi.core.server.listener.packet

import com.github.retrooper.packetevents.event.PacketListenerCommon
import dev.slne.surf.surfapi.core.api.extensions.packetEvents
import dev.slne.surf.surfapi.core.api.util.objectSetOf

object CorePacketListenerManager {
    private val listeners = objectSetOf<PacketListenerCommon>(
//        ChatPacketListener(PacketListenerPriority.NORMAL)
    )

    fun registerListeners() {
        listeners.forEach { register(it) }
    }

    fun unregisterListeners() {
        val eventManager = packetEvents.eventManager
        listeners.forEach { eventManager.unregisterListener(it) }
    }

    private fun register(listener: PacketListenerCommon) {
        packetEvents.eventManager.registerListener(listener)
    }
}
