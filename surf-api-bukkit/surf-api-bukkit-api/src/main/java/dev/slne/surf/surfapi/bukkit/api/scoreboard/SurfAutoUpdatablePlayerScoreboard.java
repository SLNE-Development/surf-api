package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

/**
 * SurfAutoUpdatablePlayerScoreboard is an interface representing an auto-updatable scoreboard that
 * includes all online players as viewers. This interface extends the SurfAutoUpdatableScoreboard
 * interface.
 */
@ApiStatus.NonExtendable
public interface SurfAutoUpdatablePlayerScoreboard extends SurfAutoUpdatableScoreboard {

  /**
   * Adds a viewer to the scoreboard. In this implementation, this method does nothing and prints a
   * warning to the console because all online players are automatically added as viewers.
   *
   * @param viewer the player to add as a viewer
   */
  @ApiStatus.Internal
  @Override
  void addViewer(Player viewer);

  /**
   * Removes a viewer from the scoreboard. In this implementation, this method does nothing and
   * prints a warning to the console because the scoreboard is always visible to all online
   * players.
   *
   * @param viewer the player to remove as a viewer
   */
  @ApiStatus.Internal
  @Override
  void removeViewer(Player viewer);
}
