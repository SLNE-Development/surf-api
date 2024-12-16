package dev.slne.surf.surfapi.core.server.listener.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import dev.slne.surf.surfapi.core.api.util.objectSetOf
import dev.slne.surf.surfapi.core.server.listener.packet.chat.ChatPacketListener

object CorePacketListenerManager {
    private val listeners = objectSetOf(ChatPacketListener(PacketListenerPriority.NORMAL))

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
