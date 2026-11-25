package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean

/**
 * The packet listeners of the selected NMS version module, such as the lore and glowing handlers.
 *
 * surf-api registers them with the packet listener API on enable and removes them on disable.
 *
 * @property listeners the listeners, in registration order
 */
@OptIn(NmsUseWithCaution::class)
class NmsPacketListeners(val listeners: List<PacketListener>) {

    companion object {
        @InternalSurfApi
        val instance by surfBean<NmsPacketListeners>()
    }
}
