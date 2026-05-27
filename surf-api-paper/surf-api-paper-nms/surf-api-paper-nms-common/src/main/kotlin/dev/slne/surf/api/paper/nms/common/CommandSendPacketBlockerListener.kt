package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@OptIn(NmsUseWithCaution::class)
abstract class CommandSendPacketBlockerListener(protected val blockedPlayers: Set<UUID>) : PacketListener {
    protected val receivedFirstCommandPacket: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun removeReceivedFirstCommandPacket(uuid: UUID) {
        receivedFirstCommandPacket.remove(uuid)
    }
}