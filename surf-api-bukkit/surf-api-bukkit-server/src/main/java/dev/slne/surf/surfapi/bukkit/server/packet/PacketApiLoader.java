package dev.slne.surf.surfapi.bukkit.server.packet;

import com.github.retrooper.packetevents.PacketEvents;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import dev.slne.surf.surfapi.bukkit.server.packet.listener.PlayerChannelInjector;
import dev.slne.surf.surfapi.bukkit.server.packet.lore.PacketLoreListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.jetbrains.annotations.ApiStatus;

/**
 * The PacketApiLoader class is responsible for loading and initializing the packet API. It sets up
 * packet events, entity lib, and entity counter.
 */
@ApiStatus.Internal
public final class PacketApiLoader {

  /**
   * Represents a BukkitMain plugin instance. This variable is used for loading the packet API.
   */
  private final BukkitMain plugin;

  /**
   * This class represents a PacketApiLoader.
   *
   * @param plugin The BukkitMain instance used for loading the packet API.
   */
  public PacketApiLoader(BukkitMain plugin) {
    this.plugin = plugin;
  }

  /**
   * Initializes the necessary components for the onLoad event.
   */
  public void onLoad() {
    setupPacketEvents();
  }

  /**
   * Initializes the plugin by calling the init method of the PacketEvents API.
   */
  public void onEnable() {
    PacketEvents.getAPI().init();
//    PacketEvents.getAPI().getEventManager().registerListener(PacketLoreListener.INSTANCE);

    SurfBukkitPacketListenerApi.get().registerListeners(PacketLoreListener.INSTANCE);

    PlayerChannelInjector.INSTANCE.register();
    plugin.getServer().getPluginManager().registerEvents(PlayerChannelInjector.INSTANCE, plugin);
  }

  /**
   * This method is called when the plugin is being disabled. It initializes the PacketEvents API.
   */
  public void onDisable() {
    PacketEvents.getAPI().terminate();
  }

  /**
   * Sets up the packet events by configuring the PacketEvents API. This method should be called
   * during the onLoad event.
   */
  private void setupPacketEvents() {
    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
    PacketEvents.getAPI().load();
  }
}
