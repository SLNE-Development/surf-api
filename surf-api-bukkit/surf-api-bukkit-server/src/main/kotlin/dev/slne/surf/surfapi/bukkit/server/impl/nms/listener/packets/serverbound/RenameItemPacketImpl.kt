package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.RenameItemPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket

@NmsUseWithCaution
class RenameItemPacketImpl(nmsPacket: ServerboundRenameItemPacket) :
    NmsServerboundPacketImpl<ServerboundRenameItemPacket>(nmsPacket), RenameItemPacket {
    override val newName: String get() = nmsPacket.name
}
