package dev.slne.surf.surfapi.core.server.config;

import SpongeConfigManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Optional;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
@Experimental
@ParametersAreNonnullByDefault
public class SurfModernConfigTracker {

  private final Object2ObjectMap<Class<?>, SpongeConfigManager<?>> configManagers;

  public SurfModernConfigTracker() {
    configManagers = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
  }

  public <C> Optional<C> getConfig(Class<C> configClass) {
    return Optional.ofNullable((SpongeConfigManager<C>) configManagers.get(configClass))
        .map(SpongeConfigManager::getConfig);
  }

  public <C> C reloadConfig(Class<C> configClass) {
    return (C) configManagers.get(configClass).reloadFromFile();
  }


  public <C> void registerConfig(Class<C> configClass, SpongeConfigManager<C> configManager) {
    configManagers.put(configClass, configManager);
  }

  public <C> SpongeConfigManager<C> getConfigManager(Class<C> configClass) {
    return (SpongeConfigManager<C>) configManagers.get(configClass);
  }
}
