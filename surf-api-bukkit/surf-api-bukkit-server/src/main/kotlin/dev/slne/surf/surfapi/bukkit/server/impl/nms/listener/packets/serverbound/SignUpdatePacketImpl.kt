package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.SignUpdatePacket
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket

@NmsUseWithCaution
class SignUpdatePacketImpl(nmsPacket: ServerboundSignUpdatePacket) :
    NmsServerboundPacketImpl<ServerboundSignUpdatePacket>(nmsPacket), SignUpdatePacket {
    override val position get() = nmsPacket.pos.toBukkit()
    override val lines: Array<String> get() = nmsPacket.lines
    override val isFrontText get() = nmsPacket.isFrontText

    override fun getLine(line: Int): String {
        require(line in 1..4) { "Line must be between 1 and 4" }
        return lines[line - 1]
    }
}
