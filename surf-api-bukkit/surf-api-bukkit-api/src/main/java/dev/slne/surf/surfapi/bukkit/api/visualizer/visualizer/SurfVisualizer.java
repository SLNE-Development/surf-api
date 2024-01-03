package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketBlockDisplay;
import me.tofaa.entitylib.meta.other.BlockDisplayMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public interface SurfVisualizer {

    Material DEFAULT_MATERIAL = Material.GLASS;

    void addVisualLocation(Location visualLocation);

    void addVisualLocation(Location visualLocation, Material material);

    void addVisualLocation(Location visualLocation, Material material, Consumer<PacketBlockDisplay> consumer);

    void removeVisualLocation(Location visualLocation);

    boolean startVisualizing();

    void addViewer(Player player);

    void removeViewer(Player player);

    boolean stopVisualizing();
}
