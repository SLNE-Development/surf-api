package dev.slne.surf.surfapi.core.api.packet;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

@ApiStatus.NonExtendable
public interface SurfCorePacketApi {

    SurfCorePacketEntityApi getPacketEntityApi();

    void sendPacket(UUID viewer, PacketWrapper<?> packet);

    static SurfCorePacketApi get() {
        return SurfCoreApi.getCore().getPacketApi();
    }
}
