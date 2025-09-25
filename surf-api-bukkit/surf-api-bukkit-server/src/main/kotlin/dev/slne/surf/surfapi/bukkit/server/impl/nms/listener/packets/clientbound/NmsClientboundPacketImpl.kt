package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.clientbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.NmsPacketImpl
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener

@NmsUseWithCaution
abstract class NmsClientboundPacketImpl<Nms : Packet<Listener>, Listener : ClientCommonPacketListener>(
    nmsPacket: Nms,
) : NmsClientboundPacket, NmsPacketImpl<Nms, Listener>(nmsPacket)