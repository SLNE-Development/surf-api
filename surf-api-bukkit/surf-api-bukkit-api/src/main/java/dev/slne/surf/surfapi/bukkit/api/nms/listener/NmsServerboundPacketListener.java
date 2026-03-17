package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListenerResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NmsUseWithCaution
@NullMarked
public interface NmsServerboundPacketListener<Packet extends NmsPacket> extends
        NmsPacketListener<Packet> {

    @OverrideOnly
    PacketListenerResult handleServerboundPacket(Packet packet, Player player);

    @OverrideOnly
    default PacketListenerResult handleEarlyServerboundPacket(Packet packet, @Nullable Player player) {
        if (player != null) {
            return handleServerboundPacket(packet, player);
        } else {
            throw new IllegalStateException(
                    "No player is available for this serverbound packet yet. " +
                            "This can happen during early connection phases such as login. " +
                            "Override handleEarlyServerboundPacket(...) if your listener should handle packets before a Player exists."
            );
        }
    }
}
