package dev.slne.surf.surfapi.velocity.server.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import org.jetbrains.annotations.ApiStatus;

/**
 * The PacketApiLoader class is responsible for loading and initializing the packet API.
 * It sets up packet events, entity lib, and entity counter.
 */
@ApiStatus.Internal
public final class PacketApiLoader {

    private final ProxyServer server;
    private final PluginContainer pluginContainer;

    /**
     * This class represents a PacketApiLoader.
     *
     * @param server The ProxyServer instance used for loading the packet API.
     * @param pluginContainer The PluginContainer instance used for loading the packet API.
     */
    public PacketApiLoader(ProxyServer server, PluginContainer pluginContainer) {
        this.server = server;
        this.pluginContainer = pluginContainer;
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
    }

    /**
     * This method is called when the plugin is being disabled.
     * It initializes the PacketEvents API.
     */
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    /**
     * Sets up the packet events by configuring the PacketEvents API.
     * This method should be called during the onLoad event.
     */
    private void setupPacketEvents() {
        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, pluginContainer));
        PacketEvents.getAPI().load();
    }
}
