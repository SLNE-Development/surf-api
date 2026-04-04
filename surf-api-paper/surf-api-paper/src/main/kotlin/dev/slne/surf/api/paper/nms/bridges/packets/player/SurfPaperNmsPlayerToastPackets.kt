package dev.slne.surf.api.paper.nms.bridges.packets.player

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.player.toast.Toast

@NmsUseWithCaution
interface SurfPaperNmsPlayerToastPackets {
    fun showToast(toast: Toast): PacketOperation

    companion object : SurfPaperNmsPlayerToastPackets by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPlayerToastPackets>()