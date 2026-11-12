@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges.packets.player

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.player.toast.Toast
import dev.slne.surf.api.shared.api.util.InternalSurfApi

@NmsUseWithCaution
interface SurfPaperNmsPlayerToastPackets {
    fun showToast(toast: Toast): PacketOperation

    companion object {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsPlayerToastPackets>()
