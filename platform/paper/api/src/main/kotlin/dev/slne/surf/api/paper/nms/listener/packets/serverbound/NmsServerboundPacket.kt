package dev.slne.surf.api.paper.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket

@NmsUseWithCaution
sealed interface NmsServerboundPacket : NmsPacket
