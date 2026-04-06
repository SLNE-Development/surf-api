package dev.slne.surf.api.paper.nms.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import net.kyori.adventure.text.Component

@NmsUseWithCaution
interface SystemChatPacket : NmsClientboundPacket {
    var content: Component
    var overlay: Boolean
}