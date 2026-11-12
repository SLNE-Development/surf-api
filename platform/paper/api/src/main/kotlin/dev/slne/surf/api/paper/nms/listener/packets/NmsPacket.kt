package dev.slne.surf.api.paper.nms.listener.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution

@NmsUseWithCaution
interface NmsPacket {
    val packetClass: Class<*>
}
