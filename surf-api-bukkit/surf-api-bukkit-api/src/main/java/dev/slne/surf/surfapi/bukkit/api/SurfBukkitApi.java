package dev.slne.surf.surfapi.bukkit.api;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Represents the API for SurfBukkit.
 */
@ApiStatus.NonExtendable
public interface SurfBukkitApi extends SurfCoreApi {

    /**
     * Retrieves the specific SurfBukkitPacketApi instance.
     *
     * @return the SurfBukkitPacketApi instance
     */
    @Override
    SurfBukkitPacketApi getPacketApi();

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
