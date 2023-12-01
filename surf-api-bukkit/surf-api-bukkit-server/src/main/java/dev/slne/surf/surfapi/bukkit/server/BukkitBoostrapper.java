package dev.slne.surf.surfapi.bukkit.server;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The BukkitBoostrapper class is responsible for bootstrapping a Bukkit plugin.
 * It implements the PluginBootstrap interface and provides the necessary methods for plugin bootstrap and creation.
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
public class BukkitBoostrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext bootstrapContext) {
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new BukkitMain();
    }
}
