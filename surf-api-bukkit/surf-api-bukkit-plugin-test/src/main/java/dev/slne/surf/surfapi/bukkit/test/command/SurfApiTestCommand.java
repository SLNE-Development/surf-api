package dev.slne.surf.surfapi.bukkit.test.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.*;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.gui.InventoryFrameworkTest;

public class SurfApiTestCommand extends CommandAPICommand {

  public SurfApiTestCommand() {
    super("surfapitest");

    withPermission("surfapitest.use");

    withSubcommands(
        new PacketLoreTest("packetlore"),
        new ScoreboardTest("scoreboard"),
        new SmoothTimeSkip("smoothtimeskip"),
        new PacketEntityTest("packetentity"),
        new ReflectionTest("reflection"),
        new PrefixConfigTest("prefixconfig"),
        new CommandExceptionTest("commandexception"),
        new InventoryFrameworkTest("inventoryframework"),
        new MaxStacksizeTest("maxstacksize"),
        new VisualizerTest("visualizer"),
        new GlowingTest("glowing"),
        new PaginationTest("pagination"),
        new InventoryTest("inventory"),
        new ToastTest("toast"),
        new SuspendCommandExecutionTest("suspendCommandExecution"),
        new SummonCommandTest("summoncommand")
    );
  }
}
