package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;


/**
 * The SurfScoreboard interface represents a scoreboard in a surf game.
 * This scoreboard can be enabled, disabled, and updated.
 * Viewers can be added and removed from the scoreboard.
 */
@ApiStatus.NonExtendable
public interface SurfScoreboard {

    /**
     * Adds a viewer to the scoreboard.
     *
     * @param viewer the player to add as a viewer
     */
    void addViewer(Player viewer);

    /**
     * Removes a viewer from the scoreboard.
     * The viewer will no longer see updates on the scoreboard
     *
     * @param viewer the player to remove as a viewer
     */
    void removeViewer(Player viewer);

    /**
     * Creates the scoreboard. This method must be called before any viewers are added.
     *
     * @throws IllegalStateException if the scoreboard is already enabled
     */
    void enable();

    /**
     * Disables the SurfScoreboard.
     * This method  closes the scoreboard.
     * It also resets the animations and the enabled flag.
     *
     * @throws IllegalStateException if the scoreboard is not enabled
     */
    void disable();

    /**
     * Updates the scoreboard. This method should be called periodically to update the contents of the scoreboard.
     * <p>
     * This method is responsible for updating any animations and applying the updated layout to the scoreboard.
     * It should only be called when the scoreboard is enabled.
     *
     * @throws IllegalStateException if the scoreboard is not enabled
     */
    void update();
}
