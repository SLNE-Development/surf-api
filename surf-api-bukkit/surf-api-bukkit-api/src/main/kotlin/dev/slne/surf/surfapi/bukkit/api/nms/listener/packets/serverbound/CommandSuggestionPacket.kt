package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution

@NmsUseWithCaution
interface CommandSuggestionPacket : NmsServerboundPacket {
    val completionId: Int
    val command: String
}
