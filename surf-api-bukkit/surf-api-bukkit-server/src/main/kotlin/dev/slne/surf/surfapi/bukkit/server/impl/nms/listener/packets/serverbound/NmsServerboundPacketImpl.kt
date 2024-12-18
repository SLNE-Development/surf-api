package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerGamePacketListener

@NmsUseWithCaution
abstract class NmsServerboundPacketImpl<Nms : Packet<ServerGamePacketListener>>(nmsPacket: Nms) :
    NmsPacketImpl<Nms, ServerGamePacketListener>(nmsPacket)
