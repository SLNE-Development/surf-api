package dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.RenameItemPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket

@NmsUseWithCaution
class RenameItemPacketImpl(nmsPacket: ServerboundRenameItemPacket) :
    NmsServerboundPacketImpl<ServerboundRenameItemPacket>(nmsPacket), RenameItemPacket {
    override val newName: String get() = nmsPacket.name
}
