package dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketBlockDisplay;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.ApiStatus;

public interface SurfPatternedVisualizer extends SurfVisualizer {

  @ApiStatus.Internal
  @ApiStatus.Obsolete
  @Override
  void addVisualLocation(Location visualLocation);

  @ApiStatus.Internal
  @ApiStatus.Obsolete
  @Override
  void addVisualLocation(Location visualLocation, Material material);

  void addVisualPoint(Location point);

  void removeVisualPoint(Location point);

  void setVisualMaterial(Material material);

  void setVisualMaterial(Material material, Consumer<PacketBlockDisplay> consumer);

  void setVisualHeight(int height);

  void setRenderAtHighestPoint(boolean renderAtHighestPoint);
}
