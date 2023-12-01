package dev.slne.surf.surfapi.velocity.server;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Plugin(
        id = "surf-api-velocity",
        name = "Surf API Velocity",
        version = "1.0.0",
        description = "The Surf API Velocity plugin.",
        authors = {
                "twisti"
        }
)
public class VelocityMain {

    /**
     * The server variable is a private final variable of type ProxyServer.
     * <p>
     * It represents the ProxyServer object associated with the VelocityMain instance.
     * <p>
     * This variable is initialized in the constructor of the VelocityMain class and is not modifiable afterwards.
     * <p>
     * This variable provides access to various methods and functionalities of the ProxyServer class.
     */
    private final ProxyServer server;
    /**
     * The Logger instance associated with the VelocityMain class.
     */
    private final Logger logger;
    /**
     * The dataDirectory variable represents the path of the data directory associated with the VelocityMain instance.
     * <p>
     * The data directory is the location where the plugin stores and retrieves data files during its execution.
     * It is typically used to store configuration files, database files, or any other kind of data required by the plugin.
     * <p>
     * This variable is declared as private and final, indicating that its value cannot be changed once it is assigned,
     * and it is only accessible within the VelocityMain class.
     * <p>
     * Example usage:
     * Path directory = dataDirectory;
     */
    private final Path dataDirectory;
    /**
     * Represents the plugin description associated with the plugin instance.
     */
    private final PluginDescription pluginDescription;
    /**
     * The pluginContainer variable represents the plugin container associated with the VelocityMain instance.
     * It holds information about the plugin, such as its ID, name, version, and description.
     * It also provides access to other important objects and services, such as the server, logger,
     * data directory, plugin description, and executor service used by the VelocityMain instance.
     * <p>
     * To retrieve the server object associated with the VelocityMain instance, use the getServer() method.
     * <p>
     * To retrieve the Logger instance associated with the VelocityMain class, use the getLogger() method.
     * <p>
     * To retrieve the data directory, use the getDataDirectory() method.
     * <p>
     * To retrieve the PluginDescription of this plugin, use the getPluginDescription() method.
     * <p>
     * To retrieve the ExecutorService used by this instance of VelocityMain, use the getExecutorService() method.
     */
    private final PluginContainer pluginContainer;
    /**
     * A variable that represents an ExecutorService used by the VelocityMain instance.
     * The ExecutorService is responsible for executing tasks asynchronously.
     */
    private final ExecutorService executorService;

    /**
     * The VelocityMain class represents the main class of the Surf API Velocity plugin. It is responsible for initializing the plugin and exposing various methods for accessing different
     * components of the plugin.
     * <p>
     * The constructor of VelocityMain initializes the main objects required by the plugin, such as the server, logger, data directory, plugin description, plugin container, and executor
     * service.
     *
     * @param server The ProxyServer object associated with the VelocityMain instance.
     * @param logger The Logger object associated with the VelocityMain instance.
     * @param dataDirectory The path of the data directory associated with the VelocityMain instance.
     * @param pluginDescription The PluginDescription object associated with the VelocityMain instance.
     * @param pluginContainer The PluginContainer object associated with the VelocityMain instance.
     * @param executorService The ExecutorService used by the VelocityMain instance.
     */
    @Inject
    public VelocityMain(ProxyServer server,
                        Logger logger,
                        @DataDirectory Path dataDirectory,
                        PluginDescription pluginDescription,
                        PluginContainer pluginContainer,
                        ExecutorService executorService) {

        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginDescription = pluginDescription;
        this.pluginContainer = pluginContainer;
        this.executorService = executorService;
    }


    /**
     * Retrieves the server object associated with the VelocityMain instance.
     *
     * @return The server object.
     */
    public ProxyServer getServer() {
        return server;
    }

    /**
     * Retrieve the Logger instance associated with this class.
     *
     * @return the Logger instance
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Retrieves the data directory.
     *
     * @return The path of the data directory.
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Returns the {@link PluginDescription} of this plugin.
     *
     * @return The PluginDescription of this plugin.
     */
    public PluginDescription getPluginDescription() {
        return pluginDescription;
    }

    /**
     * Returns the plugin container associated with this VelocityMain instance.
     *
     * @return the plugin container
     */
    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    /**
     * Retrieves the ExecutorService used by this instance of VelocityMain.
     *
     * @return the ExecutorService used by this instance of VelocityMain
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }
}
