package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import org.jetbrains.annotations.ApiStatus;

// updates automatically every 5 ticks (0.25 seconds)
@ApiStatus.NonExtendable
public interface SurfAutoUpdatableScoreboard extends SurfScoreboard {

    @ApiStatus.Internal
    @Override
    void update();
}
