package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer;

import static com.google.common.base.Preconditions.checkState;

import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketBlockDisplay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class SurfPatternedVisualizerImpl extends SurfVisualizerImpl implements
    SurfPatternedVisualizer {

  private final Set<Location> visualPoints = Collections.synchronizedSet(new HashSet<>());
  private boolean renderAtHighestPoint = true;
  private Material visualMaterial = DEFAULT_MATERIAL;
  private Consumer<PacketBlockDisplay> visualMaterialConsumer = blockDisplayMeta -> {
  };

  @Override
  public void addVisualPoint(Location point) {
    visualPoints.stream().findFirst().ifPresent(
        location -> checkState(location.getWorld().equals(point.getWorld()),
            "All points must be in the same world"));
    visualPoints.add(point.clone());

    if (running) {
      restartVisualizing();
    }
  }

  @Override
  public void removeVisualPoint(Location point) {
    visualPoints.remove(point);

    if (running) {
      restartVisualizing();
    }
  }

  @Override
  public void setVisualMaterial(Material material) {
    setVisualMaterial(material, blockDisplayMeta -> {
    });
  }

  @Override
  public void setVisualMaterial(Material material, Consumer<PacketBlockDisplay> consumer) {
    visualMaterial = material;
    visualMaterialConsumer = consumer;

    if (running) {
      restartVisualizing();
    }
  }

  @Override
  public void setVisualHeight(int height) {
    visualPoints.forEach(location -> location.setY(height));
    this.renderAtHighestPoint = false;

    if (running) {
      restartVisualizing();
    }
  }

  @Override
  public void setRenderAtHighestPoint(boolean renderAtHighestPoint) {
    this.renderAtHighestPoint = renderAtHighestPoint;
  }

  @Override
  public boolean startVisualizing() {
    restartVisualizing();
    return super.startVisualizing();
  }

  private void restartVisualizing() {
    removeAllVisualLocations();
    calculateLocationsFromPoints().forEach(
        location -> addVisualLocation(location, visualMaterial, visualMaterialConsumer));
  }

  private List<Location> calculateLocationsFromPoints() {
    // check if there are at least 2 points
    if (visualPoints.size() < 2) {
      return Collections.emptyList();
    }

    final List<Location> locations = new ArrayList<>();

    for (Location visualPoint : visualPoints) {
      if (renderAtHighestPoint) {
        locations.add(visualPoint.getWorld()
            .getHighestBlockAt(visualPoint, HeightMap.MOTION_BLOCKING_NO_LEAVES).getLocation()
            .clone().add(0, 1, 0));
      } else {
        locations.add(visualPoint);
      }
    }

    locations.addAll(formLine(locations));

    return locations;
  }

  private List<Location> formLine(List<Location> linePoints) {
    final List<Location> locations = new ArrayList<>(linePoints);

    for (int i = 0; i < linePoints.size(); i++) {
      final Location currentPoint = linePoints.get(i);
      final Location nextPoint = linePoints.get(i + 1 == linePoints.size() ? 0 : i + 1);

      final List<Location> points = walkPointAToB(currentPoint, nextPoint);
      for (Location point : points) {
        locations.add(point.clone());
      }
    }

    return locations;
  }

  /**
   * Walks from point A to point B in a straight line, generating a list of locations along the
   * way.
   *
   * @param currentPoint The starting point of the walk.
   * @param nextPoint    The destination point of the walk.
   * @return A list of locations representing the path from point A to point B.
   */
  private @NotNull List<Location> walkPointAToB(@NotNull Location currentPoint,
      @NotNull Location nextPoint) { // TODO: is the y coordinate right here?
    World world = currentPoint.getWorld();
    List<Location> locations = new ArrayList<>();

    int x1 = currentPoint.blockX();
    int y1 = currentPoint.blockY();
    int z1 = currentPoint.blockZ();
    int x2 = nextPoint.blockX();
    int y2 = nextPoint.blockY();
    int z2 = nextPoint.blockZ();

    int dx = Math.abs(x2 - x1);
    int dy = Math.abs(y2 - y1);
    int dz = Math.abs(z2 - z1);

    int sx = x1 < x2 ? 1 : -1;
    int sy = y1 < y2 ? 1 : -1;
    int sz = z1 < z2 ? 1 : -1;

    int err = dx - dy - dz;

    while (true) {
      locations.add(new Location(world, x1, y1, z1));

      if (x1 == x2 && y1 == y2 && z1 == z2) {
        break;
      }

      int e2 = 2 * err;

      if (e2 < dx) {
        err += dx;
        y1 += sy;
      }

      if (e2 > -dy) {
        err -= dy;
        x1 += sx;
      }

      if (e2 < dz) {
        err += dz;
        z1 += sz;
      }

    }

    return locations;
  }
}
