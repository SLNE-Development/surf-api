package dev.slne.surf.surfapi.core.server.impl.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

@ApiStatus.Internal
public abstract class SurfCorePacketApiImpl implements SurfCorePacketApi {

    @Override
    public void sendPacket(UUID viewer, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(SurfCoreApi.getCore().getPlayer(viewer), packet);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
