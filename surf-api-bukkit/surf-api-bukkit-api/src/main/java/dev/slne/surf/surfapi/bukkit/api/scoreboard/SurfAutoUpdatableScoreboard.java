package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import org.jetbrains.annotations.ApiStatus;

/**
 * The SurfAutoUpdatableScoreboard interface represents an auto-updatable scoreboard in a surf game.
 * This scoreboard extends the SurfScoreboard interface and adds the ability to automatically update
 * itself every 5 ticks (0.25 seconds). Viewers can be added and removed from the scoreboard.
 */
@ApiStatus.NonExtendable
public interface SurfAutoUpdatableScoreboard extends SurfScoreboard {

  /**
   * Updates the scoreboard.
   * <p>
   * This method should not be called directly as it is called automatically every 5 ticks (0.25
   * seconds).
   *
   * @throws IllegalStateException if the scoreboard is not enabled
   * @since 1.0.0
   */
  @ApiStatus.Internal
  @Override
  void update();
}
