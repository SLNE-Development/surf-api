package dev.slne.surf.api.paper.server.nms.v26_3.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.SignUpdatePacket
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toBukkit
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.world.level.block.entity.SignTextSlot

@NmsUseWithCaution
@Suppress("ClassName")
class V26_3SignUpdatePacketImpl(nmsPacket: ServerboundSignUpdatePacket) :
    V26_3NmsServerboundPacketImpl<ServerboundSignUpdatePacket>(nmsPacket), SignUpdatePacket {
    override val position get() = nmsPacket.pos.toBukkit()
    override val lines: Array<String> get() = nmsPacket.lines.toTypedArray()
    override val isFrontText get() = nmsPacket.slot == SignTextSlot.FRONT

    override fun getLine(line: Int): String {
        require(line in 1..4) { "Line must be between 1 and 4" }
        return lines[line - 1]
    }
}
