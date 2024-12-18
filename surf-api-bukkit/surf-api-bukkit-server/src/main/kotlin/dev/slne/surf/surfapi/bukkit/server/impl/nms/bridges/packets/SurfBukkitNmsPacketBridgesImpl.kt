package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.SurfBukkitNmsPacketBridges
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader

@NmsUseWithCaution
@AutoService(SurfBukkitNmsPacketBridges::class)
class SurfBukkitNmsPacketBridgesImpl : SurfBukkitNmsPacketBridges {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun createEmptyPacketOperation(): PacketOperationImpl {
        return PacketOperationImpl.empty()
    }
}
