package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class StopVisualizing extends CommandAPICommand {

  public StopVisualizing(String commandName) {
    super(commandName);

    withArguments(new StringArgument("visualizerName")
        .replaceSuggestions(ArgumentSuggestions.stringCollection(
            __ -> CreateVisualizer.getVisualizerMap().keySet())));

    executes((commandSender, commandArguments) -> {
      String visualizerName = commandArguments.getUnchecked("visualizerName");

      CreateVisualizer.getVisualizerMap().get(visualizerName).stopVisualizing();

      commandSender.sendMessage("Visualizer stopped");
    });
  }
}
