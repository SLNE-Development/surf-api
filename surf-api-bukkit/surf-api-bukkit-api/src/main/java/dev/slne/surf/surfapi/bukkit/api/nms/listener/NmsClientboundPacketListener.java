package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

@NonExtendable
@ParametersAreNonnullByDefault
public non-sealed interface NmsClientboundPacketListener<Packet extends NmsClientboundPacket> extends
    NmsPacketListener<Packet> {

  @OverrideOnly
  PacketListenerResult handleClientboundPacket(Packet packet, Player player);
}
