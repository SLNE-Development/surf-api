package dev.slne.surf.api.paper.server.nms.v26_2.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.CommandSuggestionPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2CommandSuggestionPacketImpl(nmsPacket: ServerboundCommandSuggestionPacket) :
    V26_2NmsServerboundPacketImpl<ServerboundCommandSuggestionPacket>(nmsPacket),
    CommandSuggestionPacket {
    override val completionId get() = nmsPacket.id
    override val command get() = nmsPacket.command
}
