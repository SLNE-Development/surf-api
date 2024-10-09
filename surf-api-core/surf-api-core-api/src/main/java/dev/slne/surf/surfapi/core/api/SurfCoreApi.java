package dev.slne.surf.surfapi.core.api;

import dev.slne.surf.surfapi.core.api.config.SurfConfigManager.ConfigFileNamePattern;
import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern;
import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern.ModernJsonConfigFileNamePattern;
import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern.ModernYamlConfigFileNamePattern;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The main API class for the SurfCoreApi.
 */
@ApiStatus.NonExtendable
public interface SurfCoreApi {

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

  /**
   * Retrieves the SurfCorePacketApi instance.
   * <p>
   * It may be used for all packet related operations.
   * </p>
   *
   * @return the SurfCorePacketApi instance
   */
  SurfCorePacketApi getPacketApi();

  /**
   * Sends a player to a specified server.
   *
   * @param playerUuid the UUID of the player to send
   * @param server     the name of the server to send the player to
   */
  void sendPlayerToServer(UUID playerUuid, String server);

  Optional<Object> getPlayer(@NotNull UUID playerUuid);

  @ApiStatus.Experimental
  SurfReflection getReflection();

  @Deprecated(forRemoval = true)
  <C> C createConfig(@NotNull Class<C> configClass, @NotNull Path configFolder,
      @NotNull @ConfigFileNamePattern String configFileName);

  @Deprecated(forRemoval = true)
  <C> C getConfig(@NotNull Class<C> configClass);

  @Deprecated(forRemoval = true)
  <C> C reloadConfig(@NotNull Class<C> configClass);

  <C> C createModernYamlConfig(@NotNull Class<C> configClass, @NotNull Path configFolder,
      @NotNull @ModernYamlConfigFileNamePattern String configFileName);

  <C> C createModernJsonConfig(@NotNull Class<C> configClass, @NotNull Path configFolder,
      @NotNull @ModernJsonConfigFileNamePattern String configFileName);

  <C> C getModernConfig(@NotNull Class<C> configClass);

  <C> C reloadModernConfig(@NotNull Class<C> configClass);

  <C> SurfConfigManagerModern<C> getModernConfigManager(@NotNull Class<C> configClass);

  Path getDataFolder();
}
