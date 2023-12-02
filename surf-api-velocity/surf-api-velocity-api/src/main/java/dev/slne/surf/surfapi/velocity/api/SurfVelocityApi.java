package dev.slne.surf.surfapi.velocity.api;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ExecutorService;

@ApiStatus.NonExtendable
public interface SurfVelocityApi extends SurfCoreApi {

    /**
     * Retrieves the ExecutorService instance used by the SurfVelocityApi.
     *
     * @return the ExecutorService instance
     */
    ExecutorService getExecutorService();

    static SurfVelocityApi get() {
        return SurfVelocityApiAccess.getInstance();
    }
}
