package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class StartVisualizing extends CommandAPICommand {

  public StartVisualizing(String commandName) {
    super(commandName);

    withArguments(new StringArgument("visualizerName")
        .replaceSuggestions(ArgumentSuggestions.stringCollection(
            __ -> CreateVisualizer.getVisualizerMap().keySet())));

    executes((commandSender, commandArguments) -> {
      String visualizerName = commandArguments.getUnchecked("visualizerName");

      CreateVisualizer.getVisualizerMap().get(visualizerName).startVisualizing();

      commandSender.sendMessage("Visualizer started");
    });
  }
}
