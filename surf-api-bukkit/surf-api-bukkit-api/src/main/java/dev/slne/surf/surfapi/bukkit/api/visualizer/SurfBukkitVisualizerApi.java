package dev.slne.surf.surfapi.bukkit.api.visualizer;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer;
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfBukkitVisualizerApi {

    SurfVisualizer createVisualizer();

    SurfPatternedVisualizer createPatternedVisualizer();

    static SurfBukkitVisualizerApi get() {
        return SurfBukkitApi.get().getVisualizerApi();
    }
}
