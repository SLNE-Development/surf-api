package dev.slne.surf.surfapi.velocity.server.impl;

import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
import dev.slne.surf.surfapi.velocity.api.SurfVelocityApi;
import dev.slne.surf.surfapi.velocity.api.packet.SurfVelocityPacketApi;
import dev.slne.surf.surfapi.velocity.server.VelocityMain;
import dev.slne.surf.surfapi.velocity.server.impl.packet.SurfVelocityPacketApiImpl;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ExecutorService;

/**
 * The SurfVelocityApiImpl class is an implementation of the SurfCoreApiImpl class.
 * It provides additional functionality specific to Surf Velocity.
 *
 * <p>
 * Example usage:
 * {@snippet :
 * import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
 * SurfCoreApiImpl<SurfVelocityPacketApi> surfApi = new SurfVelocityApiImpl();}
 * </p>
 */
@ApiStatus.Internal
public class SurfVelocityApiImpl extends SurfCoreApiImpl<SurfVelocityPacketApi> implements SurfVelocityApi {

    /**
     * The SurfVelocityApiImpl class is an implementation of the SurfCoreApiImpl class.
     * It provides additional functionality specific to Surf Velocity.
     * <p>
     * Example usage:
     * {@snippet : SurfCoreApiImpl<SurfVelocityPacketApi> surfApi = new SurfVelocityApiImpl();}
     */
    public SurfVelocityApiImpl() {
        super(new SurfVelocityPacketApiImpl());
    }

    @Override
    public ExecutorService getExecutorService() {
        return VelocityMain.getInstance().getExecutorService();
    }
}
