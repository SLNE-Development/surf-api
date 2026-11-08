package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.CommandSuggestionPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket

@NmsUseWithCaution
class V1_21_11CommandSuggestionPacketImpl(nmsPacket: ServerboundCommandSuggestionPacket) :
    V1_21_11NmsServerboundPacketImpl<ServerboundCommandSuggestionPacket>(nmsPacket),
    CommandSuggestionPacket {
    override val completionId get() = nmsPacket.id
    override val command get() = nmsPacket.command
}
