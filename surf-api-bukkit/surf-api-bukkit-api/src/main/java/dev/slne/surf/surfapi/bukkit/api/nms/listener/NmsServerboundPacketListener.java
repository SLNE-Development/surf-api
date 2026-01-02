package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jspecify.annotations.Nullable;

@NmsUseWithCaution
public interface NmsServerboundPacketListener<Packet extends NmsPacket> extends
        NmsPacketListener<Packet> {

    @OverrideOnly
    PacketListenerResult handleServerboundPacket(Packet packet, Player player);

    @OverrideOnly
    default PacketListenerResult handleEarlyServerboundPacket(Packet packet, @Nullable Player player) {
        if (player != null) {
            return handleServerboundPacket(packet, player);
        } else {
            return PacketListenerResult.CONTINUE;
        }
    }
}
