package dev.slne.surf.surfapi.core.server.config;

import dev.slne.surf.surfapi.core.api.config.SurfConfigManager;
import dev.slne.surf.surfapi.core.api.config.SurfConfigManager.ConfigFileNamePattern;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;

@ApiStatus.Internal
@ApiStatus.Experimental
@ParametersAreNonnullByDefault
public final class SurfConfigTracker {
    private final Object2ObjectMap<Class<?>, SurfConfigManager<?>> configManagers;

    public SurfConfigTracker() {
        configManagers = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    }

    public <C> C getConfig(Class<C> configClass) {
        return (C) configManagers.get(configClass).getConfig();
    }

    public <C> C reloadConfig(Class<C> configClass) {
        return (C) configManagers.get(configClass).reloadConfig();
    }

    public <C> void registerConfig(Class<C> configClass, Path configFolder, @ConfigFileNamePattern String configFileName) {
        configManagers.put(configClass, SurfConfigManager.create(configClass, configFolder, configFileName));
    }
}
