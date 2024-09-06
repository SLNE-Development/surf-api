package dev.slne.surf.surfapi.bukkit.test;

import dev.jorel.commandapi.CommandAPI;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection;
import dev.slne.surf.surfapi.bukkit.test.config.TestConfig;
import dev.slne.surf.surfapi.bukkit.test.config.TestConfig2;
import dev.slne.surf.surfapi.bukkit.test.listener.ChatListener;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class BukkitPluginMain extends JavaPlugin {

  @SuppressWarnings("unused")
  public static @NotNull BukkitPluginMain getInstance() {
    return getPlugin(BukkitPluginMain.class);
  }

  @Override
  public void onLoad() {
    SurfBukkitPacketListenerApi.get().registerListeners(new ChatListener());

    TestConfig config = SurfCoreApi.getCore()
        .createModernYamlConfig(TestConfig.class, getDataFolder().toPath(), "test.yml");
    TestConfig2 config2 = SurfCoreApi.getCore()
        .createModernYamlConfig(TestConfig2.class, getDataFolder().toPath(), "test2.yml");

    System.err.println(config.testLinkedList);
    System.err.println(config.testObjectLinkedList);
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
}
