package dev.slne.surf.surfapi.bukkit.server.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.service.AutoService;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.hook.SurfBukkitHookManager;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder;
import dev.slne.surf.surfapi.bukkit.api.time.SkipOperations;
import dev.slne.surf.surfapi.bukkit.api.time.TimeSkipResult;
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import dev.slne.surf.surfapi.bukkit.server.hook.SurfBukkitHookManagerImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.SurfBukkitNmsBridgeImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.packet.SurfBukkitPacketApiImpl;
import dev.slne.surf.surfapi.bukkit.server.scoreboard.SurfScoreboardBuilderImpl;
import dev.slne.surf.surfapi.bukkit.server.time.TimeHandler;
import dev.slne.surf.surfapi.bukkit.server.visualizer.SurfBukkitVisualizerApiImpl;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.util.Result;
import dev.slne.surf.surfapi.core.api.util.UtilKt;
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The SurfBukkitApiImpl class is an implementation of the SurfBukkitApi interface. It extends the
 * SurfCoreApiImpl class and provides additional functionality specific to the Bukkit platform. This
 * class provides access to the SurfBukkitApi instance. It is recommended to use the static
 * {@link SurfBukkitApi#get()} method to retrieve the instance.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * SurfBukkitApi surfApi = SurfBukkitApi.get();
 * }</pre>
 *
 * @see SurfBukkitApi
 * @see SurfCoreApiImpl
 */
@ApiStatus.Internal
@AutoService(SurfCoreApi.class)
public class SurfBukkitApiImpl extends SurfCoreApiImpl<SurfBukkitPacketApi> implements
    SurfBukkitApi {

  private final SurfBukkitVisualizerApiImpl visualizerApi;
  private final SurfBukkitNmsBridgeImpl nmsBridge;
  private final SurfBukkitHookManagerImpl hookManager;

  public SurfBukkitApiImpl() {
    super(new SurfBukkitPacketApiImpl());

    UtilKt.checkInstantiationByServiceLoader();

    this.visualizerApi = new SurfBukkitVisualizerApiImpl();
    this.nmsBridge = new SurfBukkitNmsBridgeImpl();
    this.hookManager = new SurfBukkitHookManagerImpl();
  }

  public void onEnable() {
    hookManager.onEnable();
  }

  @Override
  public SurfBukkitVisualizerApi getVisualizerApi() {
    return visualizerApi;
  }

  @Override
  public SurfBukkitNmsBridgeImpl getNmsBridge() {
    return nmsBridge;
  }

  @Override
  public SurfBukkitHookManager getHookManager() {
    return hookManager;
  }

  @Override
  public ScoreboardLibrary getScoreboardLibrary() {
    return BukkitMain.getInstance().getScoreboardLibrary();
  }

  @Override
  public SurfScoreboardBuilder createScoreboard(@NotNull Component title) {
    return new SurfScoreboardBuilderImpl(title);
  }

  @Override
  public void sendPlayerToServer(UUID playerUuid, String server) {
    checkNotNull(playerUuid, "playerUuid");
    checkNotNull(server, "server");

    final Player player = Bukkit.getPlayer(playerUuid);

    if (player != null) {
      final ByteArrayDataOutput out = ByteStreams.newDataOutput();
      out.writeUTF("Connect");
      out.writeUTF(server);

      player.sendPluginMessage(BukkitMain.getInstance(), "BungeeCord", out.toByteArray());
    }
  }

  @Override
  public Optional<Object> getPlayer(@NotNull UUID playerUuid) {
    return Optional.ofNullable(Bukkit.getPlayer(checkNotNull(playerUuid, "playerUuid")));
  }

  @Override
  public Path getDataFolder() {
    return BukkitMain.getInstance().getDataFolder().toPath();
  }

  @Override
  public Result<TimeSkipResult> skipTimeSmoothly(World world, int timeToAdd) {
    return skipTimeSmoothly(world, timeToAdd, timeToAdd / TimeHandler.DEFAULT_SKIP_AMOUNT);
  }

  @Override
  public Result<TimeSkipResult> skipTimeSmoothly(World world, long timeToAdd, long duration) {
    return TimeHandler.INSTANCE.skipTimeSmoothly(world, timeToAdd, duration);
  }

  @Override
  public Result<TimeSkipResult> skipTimeSmoothly(World world,
      SkipOperations.SkipOperation skipOperation) {
    long timeToAdd = skipOperation.timeToAdd(world);
    return skipTimeSmoothly(world, timeToAdd, timeToAdd / TimeHandler.DEFAULT_SKIP_AMOUNT);
  }

  @Override
  public Map<World, Result<TimeSkipResult>> skipTimeSmoothly(int timeToAdd) {
    final Map<World, Result<TimeSkipResult>> results = new HashMap<>();

    for (World world : Bukkit.getWorlds()) {
      results.put(world, skipTimeSmoothly(world, timeToAdd));
    }

    return results;
  }

  @Override
  public Map<World, Result<TimeSkipResult>> skipTimeSmoothly(long timeToAdd, long duration) {
    final Map<World, Result<TimeSkipResult>> results = new HashMap<>();

    for (World world : Bukkit.getWorlds()) {
      results.put(world, skipTimeSmoothly(world, timeToAdd, duration));
    }

    return results;
  }

  @Override
  public Map<World, Result<TimeSkipResult>> skipTimeSmoothly(
      SkipOperations.SkipOperation skipOperation) {
    final Map<World, Result<TimeSkipResult>> results = new HashMap<>();

    for (World world : Bukkit.getWorlds()) {
      results.put(world, skipTimeSmoothly(world, skipOperation));
    }

    return results;
  }

  public static SurfBukkitApiImpl get() {
    return (SurfBukkitApiImpl) SurfBukkitApi.get();
  }

}
