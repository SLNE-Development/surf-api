package dev.slne.surf.surfapi.bukkit.server.scoreboard;

import com.google.common.collect.Lists;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatablePlayerScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatableScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation;
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
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
        checkNotNull(text, "text");
        checkNotNull(start, "start");
        checkNotNull(end, "end");

        sidebarComponentBuilder.addAnimatedLine(createGradientAnimation(text, start.asHexString(), end.asHexString()));
        return this;
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

    @Contract("_, _, _ -> new")
    private static @NotNull SidebarAnimation<Component> createGradientAnimation(Component component, String firstHex, String secondHex) {
        final float step = 1f / 20f;
        final TagResolver.Single textPlaceholder = Placeholder.component("text", component);
        final List<Component> frames = new ArrayList<>();

        // Animation from left to right
        var phase = -1f;
        while (phase < 1) {
            frames.add(
                    MiniMessage.miniMessage().deserialize("<gradient:%s:%s:%s><text></gradient>".formatted(firstHex, secondHex, phase), textPlaceholder)
            );
            phase += step;
        }

        // Animation from right to left
        frames.addAll(Lists.reverse(frames));

        return new CollectionSidebarAnimation<>(frames);
    }
}
