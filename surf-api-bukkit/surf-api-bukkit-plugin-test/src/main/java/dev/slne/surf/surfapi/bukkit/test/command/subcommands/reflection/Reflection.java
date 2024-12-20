package dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection;

import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;

public class Reflection {

  public static final SpigotRestartCommandProxy RESTART_COMMAND;

  static {
    final SurfReflection reflection = SurfReflection.getInstance();

    RESTART_COMMAND = reflection.createProxy(SpigotRestartCommandProxy.class);
  }
}
