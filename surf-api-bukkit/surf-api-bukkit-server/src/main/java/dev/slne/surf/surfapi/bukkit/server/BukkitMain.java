package dev.slne.surf.surfapi.bukkit.server;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApiAccess;
import dev.slne.surf.surfapi.bukkit.server.impl.SurfBukkitApiImpl;
import dev.slne.surf.surfapi.bukkit.server.packet.PacketApiLoader;
import dev.slne.surf.surfapi.core.server.CoreInstance;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.*;

/**
 * The BukkitMain class is the main class for the Bukkit plugin.
 * It extends the JavaPlugin class to provide functionalities specific to Bukkit.
 * This class is responsible for the lifecycle events of the plugin, such as onLoad, onEnable, and onDisable.
 * It also provides a static method to retrieve the instance of the plugin.
 */
public class BukkitMain extends JavaPlugin {

    private final CoreInstance coreInstance = new CoreInstance();
    private final SurfBukkitApiImpl surfBukkitApi = new SurfBukkitApiImpl();
    private final PacketApiLoader packetApiLoader = new PacketApiLoader(this);
    private ScoreboardLibrary scoreboardLibrary;

    @Override
    public void onLoad() {
        packetApiLoader.onLoad();
        SurfBukkitApiAccess.setInstance(surfBukkitApi);
        coreInstance.onLoad();
    }

    @Override
    public void onEnable() {
        packetApiLoader.onEnable();
        coreInstance.onEnable();

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            getComponentLogger().warn("No packet adapter available, using NoopScoreboardLibrary...");
            scoreboardLibrary = new NoopScoreboardLibrary();
        }
    }

    @Override
    public void onDisable() {
        coreInstance.onDisable();
        packetApiLoader.onDisable();
        scoreboardLibrary.close();
    }

    /**
     * Retrieves the SurfBukkitApi instance.
     *
     * @return The SurfBukkitApi instance.
     */
    @ApiStatus.Internal
    public SurfBukkitApiImpl getSurfBukkitApi() {
        return surfBukkitApi;
    }

    /**
     * Retrieves the ScoreboardLibrary instance.
     * <p>
     * This method returns the ScoreboardLibrary instance that has been initialized in the BukkitMain class.
     * If the ScoreboardLibrary has not been initialized yet, an exception is thrown.
     *
     * @return The ScoreboardLibrary instance.
     * @throws NullPointerException if the ScoreboardLibrary has not been initialized yet.
     */
    public ScoreboardLibrary getScoreboardLibrary() {
        return checkNotNull(scoreboardLibrary, "ScoreboardLibrary has not been initialized yet! Are you trying to access it before onEnable?");
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
