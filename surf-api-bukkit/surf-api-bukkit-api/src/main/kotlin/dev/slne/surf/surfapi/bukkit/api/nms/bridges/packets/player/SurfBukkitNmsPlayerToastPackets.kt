package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.toast.Toast
import dev.slne.surf.surfapi.core.api.util.requiredService

@NmsUseWithCaution
interface SurfBukkitNmsPlayerToastPackets {

    fun showToast(toast: Toast): PacketOperation

    companion object {
        val instance = requiredService<SurfBukkitNmsPlayerToastPackets>()
    }
}

@NmsUseWithCaution
val toastPacketsBridge get() = SurfBukkitNmsPlayerToastPackets.instance