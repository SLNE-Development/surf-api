package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.SignUpdatePacket
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toBukkit
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket

@NmsUseWithCaution
class V1_21_11SignUpdatePacketImpl(nmsPacket: ServerboundSignUpdatePacket) :
    V1_21_11NmsServerboundPacketImpl<ServerboundSignUpdatePacket>(nmsPacket), SignUpdatePacket {
    override val position get() = nmsPacket.pos.toBukkit()
    override val lines: Array<String> get() = nmsPacket.lines
    override val isFrontText get() = nmsPacket.isFrontText

    override fun getLine(line: Int): String {
        require(line in 1..4) { "Line must be between 1 and 4" }
        return lines[line - 1]
    }
}
