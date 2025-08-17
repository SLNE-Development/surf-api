package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import net.kyori.adventure.text.Component

@NmsUseWithCaution
interface SystemChatPacket : NmsClientboundPacket {
    var content: Component
    var overlay: Boolean
}