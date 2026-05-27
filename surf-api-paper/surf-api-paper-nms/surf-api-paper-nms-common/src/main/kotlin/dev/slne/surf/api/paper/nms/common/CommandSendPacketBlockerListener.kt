package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import java.util.*

@OptIn(NmsUseWithCaution::class)
abstract class CommandSendPacketBlockerListener(protected val blockedPlayer: Set<UUID>) : PacketListener