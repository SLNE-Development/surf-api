package dev.slne.surf.surfapi.bukkit.api;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

@ApiStatus.NonExtendable
public interface SurfBukkitApi extends SurfCoreApi {

    /**
     * Retrieves the instance of SurfBukkitApi.
     *
     * @return the instance of SurfBukkitApi
     * @throws NullPointerException if the SurfBukkitApi instance has not been initialized yet
     */
    @Contract(pure = true)
    static SurfBukkitApi get() {
        return SurfBukkitApiAccess.getInstance();
    }
}
