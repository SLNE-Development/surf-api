package dev.slne.surf.surfapi.velocity.server.impl;

import dev.slne.surf.surfapi.core.server.SurfCoreApiImpl;
import dev.slne.surf.surfapi.velocity.api.SurfVelocityApi;
import dev.slne.surf.surfapi.velocity.server.VelocityMain;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ExecutorService;

/**
 * The SurfVelocityApiImpl class is an implementation of the SurfCoreApiImpl class.
 * It provides additional functionality specific to Surf Velocity.
 *
 * <p>
 * Example usage:
 * {@snippet : SurfCoreApiImpl surfApi = new SurfVelocityApiImpl();}
 * </p>
 */
@ApiStatus.Internal
public class SurfVelocityApiImpl extends SurfCoreApiImpl implements SurfVelocityApi {

    @Override
    public ExecutorService getExecutorService() {
        return VelocityMain.getInstance().getExecutorService();
    }
}
