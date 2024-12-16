package dev.slne.surf.surfapi.bukkit.api.util;

import java.lang.StackWalker.Option;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A utility class that extends Util and provides additional helper methods specific to Bukkit.
 */
public final class UtilBukkit {

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

  public static @Nullable JavaPlugin getCallingPlugin() {
    return getCallingPlugin(2);
  }

  public static @Nullable JavaPlugin getCallingPlugin(int depth) {
    try {
      final Class<?> callerClass = STACK_WALKER.walk(frames -> frames.skip(depth).findFirst().get().getDeclaringClass());
      return JavaPlugin.getProvidingPlugin(callerClass);
    } catch (final Exception e) {
      return null;
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
