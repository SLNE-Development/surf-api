package dev.slne.surf.surfapi.core.api;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.UUID;

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
     * Sends a player to a specified server.
     *
     * @param playerUuid the UUID of the player to send
     * @param server the name of the server to send the player to
     */
    void sendPlayerToServer(UUID playerUuid, String server);

    /**
     * Retrieves the instance of the SurfCoreApi.
     *
     * @return the instance of the SurfCoreApi
     * @throws NullPointerException if the SurfCoreApi instance has not been initialized yet
     */
    @Contract(pure = true)
    static SurfCoreApi getCore() {
        return SurfCoreApiAccess.getInstance();
    }
}
