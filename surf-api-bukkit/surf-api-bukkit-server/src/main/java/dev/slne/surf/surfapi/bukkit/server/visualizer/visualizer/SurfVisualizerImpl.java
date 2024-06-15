package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.display.PacketBlockDisplay;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SurfVisualizerImpl implements SurfVisualizer {

  protected final Object2ObjectMap<Location, Material> visualLocations = Object2ObjectMaps.synchronize(
      new Object2ObjectOpenHashMap<>());
  private final Set<UUID> viewers = Collections.synchronizedSet(new HashSet<>());
  private final Object2ObjectMap<Location, PacketBlockDisplay> entities = Object2ObjectMaps.synchronize(
      new Object2ObjectOpenHashMap<>());
  private final Object2ObjectMap<UUID, List<Location>> inDistance = Object2ObjectMaps.synchronize(
      new Object2ObjectOpenHashMap<>());
  private final Object2ObjectMap<UUID, List<Location>> oldInDistance = Object2ObjectMaps.synchronize(
      new Object2ObjectOpenHashMap<>());
  protected @Nullable VisualizerTask task;
  protected boolean running = false;

  public SurfVisualizerImpl() {
  }

  @Override
  public void addVisualLocation(Location visualLocation) {
    addVisualLocation(visualLocation, DEFAULT_MATERIAL);
  }

  @Override
  public void addVisualLocation(Location visualLocation, Material material) {
    addVisualLocation(visualLocation, material, blockDisplayMeta -> {
    });
  }

  @Override
  public void addVisualLocation(Location visualLocation, Material material,
      Consumer<PacketBlockDisplay> consumer) {
    visualLocations.put(visualLocation, material);
    entities.put(visualLocation, SurfBukkitPacketApi.get().getPacketEntityApi()
        .spawnEntity(PacketBlockDisplay.class, UUID.randomUUID(), blockDisplayMeta -> {
          blockDisplayMeta.blockState(
              SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()));
          consumer.accept(blockDisplayMeta);
        }));
  }

  @Override
  public void removeVisualLocation(Location visualLocation) {
    visualLocations.remove(visualLocation);
    PacketBlockDisplay removed = entities.remove(visualLocation);
    if (removed != null) {
//            removed.remove(); TODO
    }
  }

  protected void removeAllVisualLocations() {
    visualLocations.clear();
//        entities.values().forEach(SurfEntity::remove); TODO
    entities.clear();
  }

  @Override
  public boolean startVisualizing() {
    if (running) {
      ComponentLogger.logger().warn("Tried to start visualizing while already running!");
      return false;
    }

    if (visualLocations.isEmpty()) {
      ComponentLogger.logger().warn("Tried to start visualizing with no visual locations!");
      return false;
    }

    entities.forEach((location, blockDisplayMetaSurfEntity) -> {
      blockDisplayMetaSurfEntity.spawn(SpigotConversionUtil.fromBukkitLocation(location));
      viewers.forEach(blockDisplayMetaSurfEntity::addViewer);
    });

    task = new VisualizerTask();
    task.start();
    return true;
  }

  @Override
  public void addViewer(Player player) {
    viewers.add(player.getUniqueId());
    entities.values().forEach(
        blockDisplayMetaSurfEntity -> blockDisplayMetaSurfEntity.addViewer(player.getUniqueId()));
  }

  @Override
  public void removeViewer(Player player) {
    viewers.remove(player.getUniqueId());
    entities.values().forEach(blockDisplayMetaSurfEntity -> blockDisplayMetaSurfEntity.removeViewer(
        player.getUniqueId()));
  }

  @Override
  public boolean stopVisualizing() {
    if (!running) {
      ComponentLogger.logger().warn("Tried to stop visualizing while not running!");
      return false;
    }

    assert task != null : "Task is null while running!";

//        entities.values().forEach(SurfEntity::remove); TODO
    entities.clear();
    task.cancel();
    return true;
  }

  protected class VisualizerTask extends BukkitRunnable {

    public void start() {
      this.runTaskTimerAsynchronously(BukkitMain.getInstance(), 0, 20L);
    }

    @Override
    public void run() {
      for (UUID viewer : viewers) {
        Player player = Bukkit.getPlayer(viewer);

        if (player == null) {
          continue;
        }

        List<Location> oldLocations = inDistance.getOrDefault(viewer, Collections.emptyList());
        List<Location> newLocations = visualLocations.keySet().stream()
            .filter(location -> location.getWorld().equals(player.getWorld()))
            .filter(
                location -> location.distanceSquared(player.getLocation()) <= getSimulationDistance(
                    player))
            .toList();

        oldInDistance.put(viewer, oldLocations);
        inDistance.put(viewer, newLocations);

        // get old locations that are not in new locations
        List<Location> toRemove = newLocations.stream()
            .filter(location -> !oldLocations.contains(location))
            .toList();

        for (Location location : toRemove) {
          PacketBlockDisplay entity = entities.get(location);

          if (entity == null) {
            continue;
          }

          entity.removeViewer(player.getUniqueId());
        }
      }
    }

    private int getSimulationDistance(Player player) {
      return Math.min(Bukkit.getSimulationDistance(), player.getSimulationDistance());
    }
  }
}
