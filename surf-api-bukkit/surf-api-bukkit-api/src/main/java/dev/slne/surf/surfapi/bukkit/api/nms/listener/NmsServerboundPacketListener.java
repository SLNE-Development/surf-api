package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public non-sealed interface NmsServerboundPacketListener<Packet extends NmsServerboundPacket> extends
    NmsPacketListener<Packet> {

  @OverrideOnly
  PacketListenerResult handleServerboundPacket(Packet packet, Player player);
}
