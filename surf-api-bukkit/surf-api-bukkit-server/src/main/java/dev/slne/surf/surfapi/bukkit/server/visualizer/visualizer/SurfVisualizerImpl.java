package dev.slne.surf.surfapi.bukkit.server.visualizer.visualizer;

import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfVisualizer;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.tofaa.entitylib.meta.other.BlockDisplayMeta;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class SurfVisualizerImpl implements SurfVisualizer {

    protected final Object2ObjectMap<Location, Material> visualLocations = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    private final Set<UUID> viewers = Collections.synchronizedSet(new HashSet<>());
    private final Object2ObjectMap<Location, SurfEntity<BlockDisplayMeta>> entities = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    private final Object2ObjectMap<UUID, List<Location>> inDistance = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    private final Object2ObjectMap<UUID, List<Location>> oldInDistance = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
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
    public void addVisualLocation(Location visualLocation, Material material, Consumer<BlockDisplayMeta> consumer) {
        visualLocations.put(visualLocation, material);
        entities.put(visualLocation, SurfBukkitPacketApi.get().createEntity(UUID.randomUUID(), EntityType.BLOCK_DISPLAY, blockDisplayMeta -> {
            blockDisplayMeta.setBlockId(SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).getGlobalId());
            consumer.accept(blockDisplayMeta);
        }));
    }

    @Override
    public void removeVisualLocation(Location visualLocation) {
        visualLocations.remove(visualLocation);
        SurfEntity<BlockDisplayMeta> removed = entities.remove(visualLocation);
        if (removed != null) {
            removed.remove();
        }
    }

    protected void removeAllVisualLocations() {
        visualLocations.clear();
        entities.values().forEach(SurfEntity::remove);
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
            blockDisplayMetaSurfEntity.spawn(location);
            viewers.forEach(blockDisplayMetaSurfEntity::addViewer);
        });

        task = new VisualizerTask();
        task.start();
        return true;
    }

    @Override
    public void addViewer(Player player) {
        viewers.add(player.getUniqueId());
        entities.values().forEach(blockDisplayMetaSurfEntity -> blockDisplayMetaSurfEntity.addViewer(player));
    }

    @Override
    public void removeViewer(Player player) {
        viewers.remove(player.getUniqueId());
        entities.values().forEach(blockDisplayMetaSurfEntity -> blockDisplayMetaSurfEntity.removeViewer(player));
    }

    @Override
    public boolean stopVisualizing() {
        if (!running) {
            ComponentLogger.logger().warn("Tried to stop visualizing while not running!");
            return false;
        }

        assert task != null : "Task is null while running!";

        entities.values().forEach(SurfEntity::remove);
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
                        .filter(location -> location.distanceSquared(player.getLocation()) <= getSimulationDistance(player))
                        .toList();

                oldInDistance.put(viewer, oldLocations);
                inDistance.put(viewer, newLocations);

                // get old locations that are not in new locations
                List<Location> toRemove = newLocations.stream()
                        .filter(location -> !oldLocations.contains(location))
                        .toList();

                for (Location location : toRemove) {
                    SurfEntity<BlockDisplayMeta> entity = entities.get(location);

                    if (entity == null) {
                        continue;
                    }

                    entity.removeViewer(player);
                }
            }
        }

        private int getSimulationDistance(Player player) {
            return Math.min(Bukkit.getSimulationDistance(), player.getSimulationDistance());
        }
    }
}
