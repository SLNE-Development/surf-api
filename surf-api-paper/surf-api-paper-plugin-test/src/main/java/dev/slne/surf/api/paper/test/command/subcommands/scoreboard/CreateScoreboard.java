package dev.slne.surf.api.paper.test.command.subcommands.scoreboard;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.slne.surf.api.core.messages.Colors;
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatablePlayerScoreboard;
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder;
import net.kyori.adventure.text.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateScoreboard extends CommandAPICommand {

    private static final Map<String, SurfAutoUpdatablePlayerScoreboard> SCOREBOARDS = new HashMap<>();

    public CreateScoreboard(String commandName) {
        super(commandName);

        withArguments(new StringArgument("name"));

        executes((commandSender, commandArguments) -> {
            String name = commandArguments.getUnchecked("name");
            assert name != null;

            SurfAutoUpdatablePlayerScoreboard scoreboard = SurfScoreboardBuilder.builder(
                    Component.text(name))
                .addEmptyLine()
                .addLineSeparator()
                .addGradientLine(Component.text("Ein cooler Gradient!"), Colors.PRIMARY,
                    Colors.SECONDARY)
                .addLine(Component.text("Static Line"))
                .addAnimatedLine(Util.make(new ArrayList<>(60), components -> {
                    for (int i = 0; i < 60; i++) {
                        components.add(Component.text("Frame " + (i)));
                    }
                }))
                .addUpdatableLine(() -> Component.text("Updatable Line: " + UUID.randomUUID()))
                .buildAutoUpdatablePlayer();

            scoreboard.enable();

            SCOREBOARDS.put(name, scoreboard);
        });
    }

    @Contract(pure = true)
    public static Map<String, SurfAutoUpdatablePlayerScoreboard> getScoreboards() {
        return SCOREBOARDS;
    }
}
