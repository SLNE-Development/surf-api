package dev.slne.surf.surfapi.bukkit.test.command.subcommands.scoreboard;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatablePlayerScoreboard;
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfScoreboardBuilder;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import dev.slne.surf.surfapi.core.api.util.Util;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;

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

            SurfAutoUpdatablePlayerScoreboard scoreboard = SurfScoreboardBuilder.builder(Component.text(name))
                    .addEmptyLine()
                    .addLineSeparator()
                    .addGradientLine(Component.text("Ein cooler Gradient!"), Colors.PRIMARY, Colors.SECONDARY)
                    .addLine(Component.text("Static Line"))
                    .addAnimatedLine(Util.make(components -> {
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
