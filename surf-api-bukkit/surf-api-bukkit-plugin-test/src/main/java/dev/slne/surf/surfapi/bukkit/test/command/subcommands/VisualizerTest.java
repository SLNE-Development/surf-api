package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.visualizer.*;

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
