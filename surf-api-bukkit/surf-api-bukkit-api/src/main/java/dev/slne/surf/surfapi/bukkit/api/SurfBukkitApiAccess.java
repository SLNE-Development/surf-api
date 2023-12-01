package dev.slne.surf.surfapi.bukkit.api;

import org.jetbrains.annotations.ApiStatus;

import static com.google.common.base.Preconditions.*;

/**
 * The SurfBukkitApiAccess class provides a static access point to the SurfBukkitApi instance.
 * It allows setting and retrieving the SurfCoreApi instance.
 */
@ApiStatus.NonExtendable
public class SurfBukkitApiAccess {
    /**
     * This variable holds the instance of the SurfBukkitApi.
     * It is a private static variable with a single instance that can be accessed through the SurfBukkitApiAccess class.
     */
    private static SurfBukkitApi INSTANCE;

    /**
     * Sets the instance of the SurfBukkitApi.
     *
     * @param instance the SurfBukkitApi instance to set
     * @throws IllegalStateException if the SurfBukkitApi instance has already been initialized
     */
    @ApiStatus.Internal
    public static void setInstance(SurfBukkitApi instance) {
        checkState(INSTANCE == null, "SurfBukkitApi instance has already been initialized.");

        INSTANCE = instance;
    }

    /**
     * Retrieves the instance of SurfBukkitApi.
     *
     * @return the instance of SurfBukkitApi
     * @throws NullPointerException if the SurfBukkitApi instance has not been initialized yet
     */
    @ApiStatus.Internal
    public static SurfBukkitApi getInstance() {
        return checkNotNull(INSTANCE, "SurfBukkitApi instance has not been initialized yet.");
    }
}
