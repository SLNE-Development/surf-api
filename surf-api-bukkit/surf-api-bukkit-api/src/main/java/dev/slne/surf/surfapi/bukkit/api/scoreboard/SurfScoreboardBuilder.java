package dev.slne.surf.surfapi.bukkit.api.scoreboard;

import dev.slne.surf.surfapi.core.api.messages.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.function.Supplier;

@ApiStatus.NonExtendable
public interface SurfScoreboardBuilder {

    int DEFAULT_MAX_LINES = 15;

    static SurfScoreboardBuilder builder(@NotNull Component title) {
        return null;
    }

    SurfScoreboardBuilder maxLines(@Range(from = 1, to = 15) int maxLines);

    SurfScoreboardBuilder addLine(@NotNull Component line);

    default SurfScoreboardBuilder addEmptyLine() {
        return addLine(Component.empty());
    }

    SurfScoreboardBuilder addUpdatableLine(@NotNull Supplier<@NotNull Component> line);

    SurfScoreboardBuilder addAnimatedLine(@NotNull SidebarAnimation<@NotNull SidebarComponent> animation);

    SurfScoreboardBuilder addAnimatedLine(@NotNull List<@NotNull Component> frames);

    SurfScoreboardBuilder addGradientLine(@NotNull Component text, @NotNull TextColor start, @NotNull TextColor end);

    default SurfScoreboardBuilder addLineSeparator() {
        return addGradientLine(Component.text("--------------------"), Colors.WHITE, Colors.SPACER);
    }

    @NotNull
    SurfScoreboard build();

    @NotNull
    SurfAutoUpdatableScoreboard buildAutoUpdatable();

    @NotNull
    SurfAutoUpdatablePlayerScoreboard buildAutoUpdatablePlayer();
}
