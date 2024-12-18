package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.CommandSuggestionPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket

@NmsUseWithCaution
class CommandSuggestionPacketImpl(nmsPacket: ServerboundCommandSuggestionPacket) :
    NmsServerboundPacketImpl<ServerboundCommandSuggestionPacket>(nmsPacket),
    CommandSuggestionPacket {
    override val completionId get() = nmsPacket.id
    override val command get() = nmsPacket.command
}
