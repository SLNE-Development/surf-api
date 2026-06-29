package dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.RenameItemPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2RenameItemPacketImpl(nmsPacket: ServerboundRenameItemPacket) :
    V26_2NmsServerboundPacketImpl<ServerboundRenameItemPacket>(nmsPacket), RenameItemPacket {
    override val newName: String get() = nmsPacket.name
}
