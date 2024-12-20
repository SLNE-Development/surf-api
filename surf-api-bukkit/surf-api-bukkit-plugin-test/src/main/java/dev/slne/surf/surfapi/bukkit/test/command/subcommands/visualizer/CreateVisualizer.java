package dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.slne.surf.surfapi.bukkit.api.visualizer.SurfBukkitVisualizerApi;
import dev.slne.surf.surfapi.bukkit.api.visualizer.visualizer.SurfPatternedVisualizer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.Unit;
import org.bukkit.Material;

public class CreateVisualizer extends CommandAPICommand {

  private static final Map<String, SurfPatternedVisualizer> VISUALIZER_MAP = new ConcurrentHashMap<>();

  public CreateVisualizer(String commandName) {
    super(commandName);

    withArguments(new StringArgument("visualizerName"));

    executes((commandSender, commandArguments) -> {
      String visualizerName = commandArguments.getUnchecked("visualizerName");

      SurfPatternedVisualizer patternedVisualizer = SurfBukkitVisualizerApi.getInstance()
          .createPatternedVisualizer();
      patternedVisualizer.setVisualMaterial(Material.GREEN_STAINED_GLASS, blockDisplaySettings -> Unit.INSTANCE);
      patternedVisualizer.setRenderAtHighestPoint(true);
      patternedVisualizer.setVisualHeight(5);

      VISUALIZER_MAP.put(visualizerName, patternedVisualizer);

      commandSender.sendMessage("Visualizer created");
    });
  }

  public static Map<String, SurfPatternedVisualizer> getVisualizerMap() {
    return VISUALIZER_MAP;
  }
}
