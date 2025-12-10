package dev.slne.surf.surfapi.bukkit.test;

import dev.jorel.commandapi.CommandAPI;
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.InventoryFrameworkExtensions;
import dev.slne.surf.surfapi.bukkit.api.packet.listener.SurfBukkitPacketListenerApi;
import dev.slne.surf.surfapi.bukkit.test.command.SurfApiTestCommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection;
import dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig;
import dev.slne.surf.surfapi.bukkit.test.listener.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class BukkitPluginMain extends JavaPlugin {

  @SuppressWarnings("unused")
  public static @NotNull BukkitPluginMain getInstance() {
    return getPlugin(BukkitPluginMain.class);
  }

  @Override
  public void onLoad() {
    ModernTestConfig.Companion.init();
    ModernTestConfig.Companion.randomise();

    SurfBukkitPacketListenerApi.Companion.getInstance().registerListeners(new ChatListener());
    InventoryFrameworkExtensions.register(TestInventoryView.INSTANCE);
//    TestConfig config = SurfCore#Api.getCore()
//        .createModernYamlConfig(TestConfig.class, getDataFolder().toPath(), "test.yml");
//    TestConfig2 config2 = SurfCoreApi.getCore()
//        .createModernYamlConfig(TestConfig2.class, getDataFolder().toPath(), "test2.yml");
//
//    System.err.println(config.testLinkedList);
//    System.err.println(config.testObjectLinkedList);
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
