package dev.slne.surf.surfapi.bukkit.server;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApiAccess;
import dev.slne.surf.surfapi.bukkit.server.impl.SurfBukkitApiImpl;
import dev.slne.surf.surfapi.core.api.SurfCoreApiAccess;
import dev.slne.surf.surfapi.core.server.CoreInstance;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The BukkitMain class is the main class for the Bukkit plugin.
 * It extends the JavaPlugin class to provide functionalities specific to Bukkit.
 * This class is responsible for the lifecycle events of the plugin, such as onLoad, onEnable, and onDisable.
 * It also provides a static method to retrieve the instance of the plugin.
 */
public class BukkitMain extends JavaPlugin {

    private final CoreInstance coreInstance = new CoreInstance();
    private final SurfBukkitApiImpl surfBukkitApi = new SurfBukkitApiImpl();

    @Override
    public void onLoad() {
        SurfBukkitApiAccess.setInstance(surfBukkitApi);
        coreInstance.onLoad();


    }

    @Override
    public void onEnable() {
        coreInstance.onEnable();


    }

    @Override
    public void onDisable() {
        coreInstance.onDisable();


    }


    /**
     * Retrieves the instance of the BukkitMain class.
     * <p>
     * This method returns the instance of the BukkitMain class, which is the main class for the Bukkit plugin.
     * It is a static method, so it can be accessed without creating an instance of the class.
     *
     * @return The instance of the BukkitMain class.
     */
    public static @NotNull BukkitMain getInstance() {
        return getPlugin(BukkitMain.class);
    }
}
