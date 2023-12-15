package dev.slne.surf.surfapi.bukkit.api;

import dev.slne.surf.surfapi.core.api.SurfCoreApiAccess;
import org.jetbrains.annotations.ApiStatus;

import static com.google.common.base.Preconditions.*;

/**
 * The SurfBukkitApiAccess class provides a static access point to the SurfBukkitApi instance.
 * It allows setting and retrieving the SurfCoreApi instance.
 */
@ApiStatus.NonExtendable
@ApiStatus.Internal
public final class SurfBukkitApiAccess extends SurfCoreApiAccess {

    /**
     * Sets the instance of the SurfBukkitApi.
     *
     * @param instance the SurfBukkitApi instance to set
     * @throws IllegalStateException if the SurfBukkitApi instance has already been initialized
     */
    @ApiStatus.Internal
    public static void setInstance(SurfBukkitApi instance) {
        SurfCoreApiAccess.setInstance(instance);
    }

    /**
     * Retrieves the instance of SurfBukkitApi.
     *
     * @return the instance of SurfBukkitApi
     * @throws NullPointerException if the SurfBukkitApi instance has not been initialized yet
     */
    @ApiStatus.Internal
    protected static SurfBukkitApi getInstance() {
        return (SurfBukkitApi) checkNotNull(SurfCoreApiAccess.getInstance());
    }
}
