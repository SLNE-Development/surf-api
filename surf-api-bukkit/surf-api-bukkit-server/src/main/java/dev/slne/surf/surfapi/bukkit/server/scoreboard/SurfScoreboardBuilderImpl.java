package dev.slne.surf.surfapi.bukkit.server.scoreboard;

import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatablePlayerScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatableScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.*;

@ApiStatus.Internal
public class SurfScoreboardBuilderImpl implements SurfScoreboardBuilder {

    private final Component title;
    private final SidebarComponent.Builder sidebarComponentBuilder;
    private final List<CollectionSidebarAnimation<Component>> animations = new ArrayList<>();
    private int maxLines = DEFAULT_MAX_LINES;

    public SurfScoreboardBuilderImpl(@NotNull Component title) {
        checkNotNull(title, "title");

        this.title = title;
        this.sidebarComponentBuilder = SidebarComponent.builder();
    }

    @Override
    public SurfScoreboardBuilder maxLines(int maxLines) {
        checkArgument(maxLines >= 1 && maxLines <= 15, "maxLines must be between 1 and 15");

        this.maxLines = maxLines;
        return this;
    }

    @Override
    public SurfScoreboardBuilder addLine(@NotNull Component line) {
        checkNotNull(line, "line");

        sidebarComponentBuilder.addStaticLine(line);
        return this;
    }

    @Override
    public SurfScoreboardBuilder addUpdatableLine(@NotNull Supplier<@NotNull Component> line) {
        checkNotNull(line, "line");

        sidebarComponentBuilder.addDynamicLine(line);
        return this;
    }

    @Override
    public SurfScoreboardBuilder addAnimatedLine(@NotNull SidebarAnimation<@NotNull SidebarComponent> animation) {
        checkNotNull(animation, "animation");
        
        sidebarComponentBuilder.addAnimatedComponent(animation);
        return this;
    }

    @Override
    public SurfScoreboardBuilder addAnimatedLine(@NotNull List<@NotNull Component> frames) {
        checkNotNull(frames, "frames");
        checkArgument(!frames.isEmpty(), "frames cannot be empty");

        CollectionSidebarAnimation<@NotNull Component> animation = new CollectionSidebarAnimation<>(frames);
        sidebarComponentBuilder.addAnimatedLine(animation);
        animations.add(animation);
        return this;
    }

    @Override
    public SurfScoreboardBuilder addGradientLine(@NotNull Component text, @NotNull TextColor start, @NotNull TextColor end) {
        return null;
    }

    @Override
    public @NotNull SurfScoreboard build() {
        return new SurfScoreboardImpl(title, maxLines, sidebarComponentBuilder.build(), animations);
    }

    @Override
    public @NotNull SurfAutoUpdatableScoreboard buildAutoUpdatable() {
        return new SurfAutoUpdatableScoreboardImpl(title, maxLines, sidebarComponentBuilder.build(), animations);
    }

    @Override
    public @NotNull SurfAutoUpdatablePlayerScoreboard buildAutoUpdatablePlayer() {
        return new SurfAutoUpdatablePlayerScoreboardImpl(title, maxLines, sidebarComponentBuilder.build(), animations);
    }
}
