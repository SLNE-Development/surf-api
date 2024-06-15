package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer;
import org.bukkit.Location;

public class AddVisualizerPoint extends CommandAPICommand {

  public AddVisualizerPoint(String commandName) {
    super(commandName);

    withArguments(new StringArgument("visualizerName")
            .replaceSuggestions(ArgumentSuggestions.stringCollection(
                __ -> CreateVisualizer.getVisualizerMap().keySet())),
        new LocationArgument("pointLocation", LocationType.PRECISE_POSITION, false));

    executes((commandSender, commandArguments) -> {
      String visualizerName = commandArguments.getUnchecked("visualizerName");
      Location pointLocation = commandArguments.getUnchecked("pointLocation");

      SurfPatternedVisualizer surfVisualizer = CreateVisualizer.getVisualizerMap()
          .get(visualizerName);

      surfVisualizer.addVisualPoint(pointLocation);

      commandSender.sendMessage("Visualizer point added");
    });
  }
}
