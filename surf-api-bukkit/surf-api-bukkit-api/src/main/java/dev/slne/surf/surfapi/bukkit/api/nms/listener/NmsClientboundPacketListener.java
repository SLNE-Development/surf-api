package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import org.bukkit.entity.Player;

@NmsUseWithCaution
public interface NmsClientboundPacketListener<Packet extends NmsPacket> extends
    NmsPacketListener<Packet> {

  PacketListenerResult handleClientboundPacket(Packet packet, Player player);
}
