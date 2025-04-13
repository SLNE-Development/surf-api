package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import net.kyori.adventure.text.Component

@NmsUseWithCaution
interface DisconnectPacket : NmsClientboundPacket {
    val reason: Component
}