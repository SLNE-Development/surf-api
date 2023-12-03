package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

// updates automatically every 5 ticks (0.25 seconds) and adds all online players to the scoreboard
@ApiStatus.NonExtendable
public interface SurfAutoUpdatablePlayerScoreboard extends SurfAutoUpdatableScoreboard {

    @ApiStatus.Internal
    @Override
    void addViewer(Player viewer);

    @ApiStatus.Internal
    @Override
    void removeViewer(Player viewer);
}
