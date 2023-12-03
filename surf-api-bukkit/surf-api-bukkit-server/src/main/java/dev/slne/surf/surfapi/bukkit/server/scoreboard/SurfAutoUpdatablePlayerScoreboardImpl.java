package dev.slne.surf.surfapi.bukkit.server.scoreboard;

import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatablePlayerScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public class SurfAutoUpdatablePlayerScoreboardImpl extends SurfAutoUpdatableScoreboardImpl implements SurfAutoUpdatablePlayerScoreboard {

    public SurfAutoUpdatablePlayerScoreboardImpl(Component title, int maxLines, SidebarComponent sidebarComponent, List<CollectionSidebarAnimation<Component>> animations) {
        super(title, maxLines, sidebarComponent, animations);
    }

    @Override
    public void addViewer(Player viewer) {
        ComponentLogger.logger().warn("You are not allowed to add viewers to this scoreboard. This Scoreboard automatically adds viewers.");
    }

    @Override
    public void removeViewer(Player viewer) {
        ComponentLogger.logger().warn("You are not allowed to remove viewers from this scoreboard. This Scoreboard automatically removes viewers.");
    }

    @Override
    public void update() {
        super.update();
        Bukkit.getOnlinePlayers().forEach(this::addViewer);
    }
}
