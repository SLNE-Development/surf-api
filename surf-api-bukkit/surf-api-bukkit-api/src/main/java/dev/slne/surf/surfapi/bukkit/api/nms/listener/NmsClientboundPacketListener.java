package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NmsUseWithCaution
@NullMarked
public interface NmsClientboundPacketListener<Packet extends NmsPacket> extends
        NmsPacketListener<Packet> {

    @ApiStatus.OverrideOnly
    PacketListenerResult handleClientboundPacket(Packet packet, Player player);

    @ApiStatus.OverrideOnly
    default PacketListenerResult handleEarlyClientboundPacket(Packet packet, @Nullable Player player) {
        if (player != null) {
            return handleClientboundPacket(packet, player);
        } else {
            return PacketListenerResult.CONTINUE;
        }
    }
}
