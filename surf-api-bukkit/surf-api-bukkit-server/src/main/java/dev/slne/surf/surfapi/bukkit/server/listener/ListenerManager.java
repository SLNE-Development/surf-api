package dev.slne.surf.surfapi.bukkit.server.listener;

import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

/**
 * The ListenerManager class is responsible for registering and unregistering listeners in the
 * Bukkit plugin. It provides methods to register and unregister listeners for specific events. The
 * listener manager is instantiated with a reference to the main plugin class, BukkitMain.
 *
 * @see BukkitMain
 */
@ApiStatus.Internal
public class ListenerManager {

  private final BukkitMain plugin;

  /**
   * The ListenerManager class is responsible for registering and unregistering listeners in the
   * Bukkit plugin. It provides methods to register and unregister listeners for specific events.
   * The listener manager is instantiated with a reference to the main plugin class, BukkitMain.
   *
   * @see BukkitMain
   */
  public ListenerManager(BukkitMain plugin) {
    this.plugin = plugin;
  }

  /**
   * Registers all listeners.
   */
  public void registerListeners() {
    Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
  }

  /**
   * Unregisters all listeners.
   */
  public void unregisterListeners() {
    Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord");
  }
}
