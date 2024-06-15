package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.scoreboard.CreateScoreboard;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.scoreboard.StartScoreboard;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.scoreboard.StopScoreboard;

public class ScoreboardTest extends CommandAPICommand {

  public ScoreboardTest(String commandName) {
    super(commandName);

    withSubcommands(
        new CreateScoreboard("create"),
        new StartScoreboard("start"),
        new StopScoreboard("stop")
    );
  }
}
