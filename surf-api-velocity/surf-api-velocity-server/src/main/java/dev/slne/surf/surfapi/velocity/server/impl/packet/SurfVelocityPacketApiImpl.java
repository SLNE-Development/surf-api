package dev.slne.surf.surfapi.velocity.server.impl.packet;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketApiImpl;
import dev.slne.surf.surfapi.velocity.api.packet.SurfVelocityPacketApi;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SurfVelocityPacketApiImpl extends SurfCorePacketApiImpl implements SurfVelocityPacketApi {
    @Override
    public SurfCorePacketEntityApi getPacketEntityApi() {
        return null; // TODO
    }
}
