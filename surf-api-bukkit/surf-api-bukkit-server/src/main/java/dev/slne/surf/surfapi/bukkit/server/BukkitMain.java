package dev.slne.surf.surfapi.bukkit.server;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import dev.slne.surf.surfapi.bukkit.server.impl.SurfBukkitApiImpl;
import dev.slne.surf.surfapi.bukkit.server.libs.LibLoader;
import dev.slne.surf.surfapi.bukkit.server.listener.ListenerManager;
import dev.slne.surf.surfapi.bukkit.server.packet.PacketApiLoader;
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection;
import dev.slne.surf.surfapi.core.server.CoreInstance;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The BukkitMain class is the main class for the Bukkit plugin. It extends the JavaPlugin class to
 * provide functionalities specific to Bukkit. This class is responsible for the lifecycle events of
 * the plugin, such as onLoad, onEnable, and onDisable. It also provides a static method to retrieve
 * the instance of the plugin.
 */
public class BukkitMain extends JavaPlugin {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final CoreInstance coreInstance = new CoreInstance();
  private final PacketApiLoader packetApiLoader = new PacketApiLoader(this);
  private final ListenerManager listenerManager = new ListenerManager(this);

  private ScoreboardLibrary scoreboardLibrary;

  /**
   * Retrieves the instance of the BukkitMain class.
   * <p>
   * This method returns the instance of the BukkitMain class, which is the main class for the
   * Bukkit plugin. It is a static method, so it can be accessed without creating an instance of the
   * class.
   *
   * @return The instance of the BukkitMain class.
   */
  public static @NotNull BukkitMain getInstance() {
    return getPlugin(BukkitMain.class);
  }

  @Override
  public void onLoad() {
    packetApiLoader.onLoad();
    Reflection.class.getClassLoader(); // initialize Reflection

    new LibLoader(getClassLoader()).loadLibs();

    coreInstance.onLoad();
  }

  @Override
  public void onEnable() {
    listenerManager.registerListeners();
    packetApiLoader.onEnable();
    coreInstance.onEnable();
    SurfBukkitApiImpl.get().onEnable();

    try {
      scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
    } catch (NoPacketAdapterAvailableException e) {
      logger.atSevere()
          .withCause(e)
          .log("No packet adapter available, using NoopScoreboardLibrary...");
      scoreboardLibrary = new NoopScoreboardLibrary();
    }
  }

  @Override
  public void onDisable() {
    coreInstance.onDisable();
    packetApiLoader.onDisable();
    scoreboardLibrary.close();
    listenerManager.unregisterListeners();
  }


  /**
   * Retrieves the ScoreboardLibrary instance.
   * <p>
   * This method returns the ScoreboardLibrary instance that has been initialized in the BukkitMain
   * class. If the ScoreboardLibrary has not been initialized yet, an exception is thrown.
   *
   * @return The ScoreboardLibrary instance.
   * @throws NullPointerException if the ScoreboardLibrary has not been initialized yet.
   */
  public ScoreboardLibrary getScoreboardLibrary() {
    return checkNotNull(scoreboardLibrary,
        "ScoreboardLibrary has not been initialized yet! Are you trying to access it before onEnable?");
  }
}
