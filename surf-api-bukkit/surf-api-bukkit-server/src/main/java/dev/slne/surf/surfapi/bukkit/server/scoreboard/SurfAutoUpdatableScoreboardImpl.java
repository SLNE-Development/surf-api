package dev.slne.surf.surfapi.bukkit.server.scoreboard;

import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatableScoreboard;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public class SurfAutoUpdatableScoreboardImpl extends SurfScoreboardImpl implements SurfAutoUpdatableScoreboard {

    private final BukkitRunnable updater;

    public SurfAutoUpdatableScoreboardImpl(Component title, int maxLines, SidebarComponent sidebarComponent, List<CollectionSidebarAnimation<Component>> animations) {
        super(title, maxLines, sidebarComponent, animations);

        updater = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
    }

    @Override
    public void enable() {
        super.enable();
        updater.runTaskTimerAsynchronously(BukkitMain.getInstance(), 0, 5);
    }

    @Override
    public void disable() {
        updater.cancel();
        super.disable();
    }
}
