package dev.slne.surf.api.paper.test.command.subcommands.scoreboard;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatablePlayerScoreboard;

public class StopScoreboard extends CommandAPICommand {

    public StopScoreboard(String commandName) {
        super(commandName);

        withArguments(new StringArgument("name")
            .replaceSuggestions(ArgumentSuggestions.strings(
                __ -> CreateScoreboard.getScoreboards().keySet().toArray(String[]::new))));

        executes((commandSender, commandArguments) -> {
            String name = commandArguments.getUnchecked("name");
            assert name != null;

            SurfAutoUpdatablePlayerScoreboard scoreboard = CreateScoreboard.getScoreboards()
                .get(name);

            assert scoreboard != null;
            scoreboard.disable();
        });
    }
}
