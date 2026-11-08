package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.RenameItemPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket

@NmsUseWithCaution
class V1_21_11RenameItemPacketImpl(nmsPacket: ServerboundRenameItemPacket) :
    V1_21_11NmsServerboundPacketImpl<ServerboundRenameItemPacket>(nmsPacket), RenameItemPacket {
    override val newName: String get() = nmsPacket.name
}
