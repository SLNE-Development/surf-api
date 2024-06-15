package dev.slne.surf.surfapi.bukkit.server.util;

import dev.slne.surf.surfapi.core.api.util.Util;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * UtilVelocity is a utility class that extends the Util class and provides additional helper
 * methods for working with velocity.
 */
@ApiStatus.NonExtendable
public final class UtilVelocity extends Util {

  /**
   * Default constructor for the UtilVelocity class.
   * <p>
   * This constructor throws an UnsupportedOperationException as the UtilVelocity class cannot be
   * instantiated.
   */
  @Contract(" -> fail")
  private UtilVelocity() {
    super();
  }
}
