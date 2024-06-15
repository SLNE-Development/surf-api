package dev.slne.surf.surfapi.bukkit.server.scoreboard;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboard;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.FramedSidebarAnimation;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class SurfScoreboardImpl implements SurfScoreboard {

  protected final Component title;
  protected final int maxLines;
  protected final SidebarComponent sidebarComponent;
  protected final List<SidebarAnimation<Component>> animations;
  protected @Nullable Sidebar scoreboard;
  protected @Nullable ComponentSidebarLayout sidebarLayout;
  protected boolean enabled = false;

  public SurfScoreboardImpl(Component title, int maxLines, SidebarComponent sidebarComponent,
      List<SidebarAnimation<Component>> animations) {
    this.title = title;
    this.maxLines = maxLines;
    this.sidebarComponent = sidebarComponent;
    this.animations = animations;
  }

  @Override
  public void addViewer(Player viewer) {
    checkNotNull(viewer, "viewer");
    checkState(enabled, "Scoreboard is not enabled. Did you forget to call enable()?");

    assert scoreboard != null : "scoreboard is null";
    scoreboard.addPlayer(viewer);
  }

  @Override
  public void removeViewer(Player viewer) {
    checkNotNull(viewer, "viewer");
    checkState(enabled, "Scoreboard is not enabled. Did you forget to call enable()?");

    assert scoreboard != null : "scoreboard is null";
    scoreboard.removePlayer(viewer);
  }

  @Override
  public void enable() {
    checkState(!enabled, "Scoreboard is already enabled");

    scoreboard = SurfBukkitApi.get().getScoreboardLibrary().createSidebar(maxLines);
    sidebarLayout = new ComponentSidebarLayout(SidebarComponent.staticLine(title),
        sidebarComponent);

    sidebarLayout.apply(scoreboard);

    enabled = true;
  }

  @Override
  public void disable() {
    checkState(enabled, "Scoreboard is not enabled. Did you forget to call enable()?");

    assert scoreboard != null : "scoreboard is null";

    scoreboard.close();
    animations.forEach(frameSwitcher -> {
      if (frameSwitcher instanceof FramedSidebarAnimation<Component> animation) {
        animation.switchFrame(0);
      }
    });

    scoreboard = null;
    sidebarLayout = null;
    enabled = false;
  }

  @Override
  public void update() {
    checkState(enabled, "Scoreboard is not enabled. Did you forget to call enable()?");

    assert scoreboard != null : "scoreboard is null";
    assert sidebarLayout != null : "sidebarLayout is null";

    animations.forEach(SidebarAnimation::nextFrame);
    sidebarLayout.apply(scoreboard);
  }
}
