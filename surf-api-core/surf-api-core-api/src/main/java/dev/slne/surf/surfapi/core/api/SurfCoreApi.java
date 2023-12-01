package dev.slne.surf.surfapi.core.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

@ApiStatus.NonExtendable
public interface SurfCoreApi {

    /**
     * Retrieves the instance of the SurfCoreApi.
     *
     * @return the instance of the SurfCoreApi
     * @throws NullPointerException if the SurfCoreApi instance has not been initialized yet
     */
    @Contract(pure = true)
    static SurfCoreApi get() {
        return SurfCoreApiAccess.getInstance();
    }
}
