package dev.slne.surf.surfapi.core.server.config;

import dev.slne.surf.surfapi.core.api.config.SurfConfigManagerModern;
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

  private final Object2ObjectMap<Class<?>, SurfConfigManagerModern<?>> configManagers;

  public SurfModernConfigTracker() {
    configManagers = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
  }

  public <C> Optional<C> getConfig(Class<C> configClass) {
    return Optional.ofNullable((SurfConfigManagerModern<C>) configManagers.get(configClass))
        .map(SurfConfigManagerModern::getConfig);
  }

  public <C> C reloadConfig(Class<C> configClass) {
    return (C) configManagers.get(configClass).reloadFromFile();
  }


  public <C> void registerConfig(Class<C> configClass, SurfConfigManagerModern<C> configManager) {
    configManagers.put(configClass, configManager);
  }
}
