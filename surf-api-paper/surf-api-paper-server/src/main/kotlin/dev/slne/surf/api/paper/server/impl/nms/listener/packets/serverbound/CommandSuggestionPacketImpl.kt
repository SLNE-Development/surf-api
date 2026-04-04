package dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.CommandSuggestionPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket

@NmsUseWithCaution
class CommandSuggestionPacketImpl(nmsPacket: ServerboundCommandSuggestionPacket) :
    NmsServerboundPacketImpl<ServerboundCommandSuggestionPacket>(nmsPacket),
    CommandSuggestionPacket {
    override val completionId get() = nmsPacket.id
    override val command get() = nmsPacket.command
}
