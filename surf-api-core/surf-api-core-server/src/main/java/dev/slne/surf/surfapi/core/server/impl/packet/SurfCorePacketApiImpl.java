package dev.slne.surf.surfapi.core.server.impl.packet;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public abstract class SurfCorePacketApiImpl implements SurfCorePacketApi {

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
