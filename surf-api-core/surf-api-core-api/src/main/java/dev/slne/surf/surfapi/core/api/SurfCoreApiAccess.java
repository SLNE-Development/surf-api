package dev.slne.surf.surfapi.core.api;

import org.jetbrains.annotations.ApiStatus;

import static com.google.common.base.Preconditions.*;

/**
 * The SurfCoreApiAccess class provides a static access point to the SurfCoreApi instance.
 * It allows setting and retrieving the SurfCoreApi instance.
 */
@ApiStatus.NonExtendable
@ApiStatus.Internal
public class SurfCoreApiAccess {
    /**
     * This variable holds the instance of the SurfCoreApi.
     * It is a private static variable with a single instance that can be accessed through the SurfCoreApiAccess class.
     */
    private static SurfCoreApi INSTANCE;

    /**
     * Sets the instance of the SurfCoreApi.
     *
     * @param instance the SurfCoreApi instance to set
     * @throws IllegalStateException if the SurfCoreApi instance has already been initialized
     */
    @ApiStatus.Internal
    protected static void setInstance(SurfCoreApi instance) {
        checkState(INSTANCE == null, "SurfApi instance has already been initialized.");

        INSTANCE = instance;
    }

    /**
     * Retrieves the instance of SurfCoreApi.
     *
     * @return the instance of SurfCoreApi
     * @throws NullPointerException if the SurfCoreApi instance has not been initialized yet
     */
    @ApiStatus.Internal
    protected static SurfCoreApi getInstance() {
        return checkNotNull(INSTANCE, "SurfApi instance has not been initialized yet.");
    }
}
