package dev.slne.surf.surfapi.core.api.packet;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfCorePacketApi {

    SurfCorePacketEntityApi getPacketEntityApi();

    static SurfCorePacketApi get() {
        return SurfCoreApi.getCore().getPacketApi();
    }
}
