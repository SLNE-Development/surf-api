package dev.slne.surf.surfapi.velocity.api.packet;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.velocity.api.SurfVelocityApi;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfVelocityPacketApi extends SurfCorePacketApi {

    static SurfVelocityPacketApi get() {
        return SurfVelocityApi.get().getPacketApi();
    }
}
