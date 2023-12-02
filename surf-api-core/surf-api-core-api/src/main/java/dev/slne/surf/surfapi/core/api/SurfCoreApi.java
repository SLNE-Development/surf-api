package dev.slne.surf.surfapi.core.api;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * The main API class for the SurfCoreApi.
 */
@ApiStatus.NonExtendable
public interface SurfCoreApi {

    /**
     * Retrieves the SurfCorePacketApi instance.
     * <p>
     *     It may be used for all packet related operations.
     * </p>
     *
     * @return the SurfCorePacketApi instance
     */
    SurfCorePacketApi getPacketApi();

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
