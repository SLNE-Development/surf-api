package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution

@NmsUseWithCaution
interface NmsPacket {
    val packetClass: Class<*>
}
