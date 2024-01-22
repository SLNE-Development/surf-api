package dev.slne.surf.surfapi.core.api;

import dev.slne.surf.surfapi.core.api.config.SurfConfigManager.ConfigFileNamePattern;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
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

    Optional<Object> getPlayer(@NotNull UUID playerUuid);

    @ApiStatus.Experimental
    SurfReflection getReflection();

    <C> C createConfig(@NotNull Class<C> configClass, @NotNull Path configFolder, @NotNull @ConfigFileNamePattern String configFileName);

    <C> C getConfig(@NotNull Class<C> configClass);

    <C> C reloadConfig(@NotNull Class<C> configClass);

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
