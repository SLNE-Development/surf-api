package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.AddVisualizerPoint;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.CreateVisualizer;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.RemoveVisualizerPoint;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.StartVisualizing;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.StopVisualizing;

public class VisualizerTest extends CommandAPICommand {

  public VisualizerTest(String commandName) {
    super(commandName);

    withSubcommands(
        new CreateVisualizer("create"),
        new AddVisualizerPoint("addpoint"),
        new RemoveVisualizerPoint("removepoint"),
        new StartVisualizing("start"),
        new StopVisualizing("stop")
    );
  }
}
