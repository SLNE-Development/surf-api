package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfScoreboard {

    void addViewer(Player viewer);

    void removeViewer(Player viewer);

    void enable();

    void disable();

    void update();
}
