package dev.slne.surf.surfapi.bukkit.api.util;

import dev.slne.surf.surfapi.core.api.util.Util;
import java.lang.StackWalker.Option;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class that extends Util and provides additional helper methods specific to Bukkit.
 */
public final class UtilBukkit extends Util {

  // @formatter:off
  private static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
  // @formatter:on


  @Contract("_ -> new")
  public static @NotNull NamespacedKey key(String name) {
    try {
      final Class<?> callerClass = STACK_WALKER.getCallerClass();
      final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(callerClass);

      return new NamespacedKey(plugin, name);
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to get the providing plugin", e);
    }
  }

  /**
   * A utility class that extends Util and provides additional helper methods specific to Bukkit.
   */
  @Contract(" -> fail")
  private UtilBukkit() {
    super();
  }
}
