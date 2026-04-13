package dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.RenameItemPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket

@NmsUseWithCaution
class V26_1RenameItemPacketImpl(nmsPacket: ServerboundRenameItemPacket) :
    V26_1NmsServerboundPacketImpl<ServerboundRenameItemPacket>(nmsPacket), RenameItemPacket {
    override val newName: String get() = nmsPacket.name
}
