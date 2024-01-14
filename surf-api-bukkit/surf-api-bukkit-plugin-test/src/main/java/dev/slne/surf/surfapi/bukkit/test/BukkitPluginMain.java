package dev.slne.surf.surfapi.bukkit.test;

import dev.jorel.commandapi.CommandAPI;
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class BukkitPluginMain extends JavaPlugin {

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        new SurfApiTestCommand().register();
        Reflection.class.getClassLoader(); // initialize Reflection
    }

    @Override
    public void onDisable() {
        CommandAPI.unregister("surfapitest");
    }

    public static @NotNull BukkitPluginMain getInstance() {
        return getPlugin(BukkitPluginMain.class);
    }
}
