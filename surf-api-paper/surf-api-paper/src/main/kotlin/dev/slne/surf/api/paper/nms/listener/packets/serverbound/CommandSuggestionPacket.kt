package dev.slne.surf.api.paper.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution

@NmsUseWithCaution
interface CommandSuggestionPacket : NmsServerboundPacket {
    val completionId: Int
    val command: String
}
