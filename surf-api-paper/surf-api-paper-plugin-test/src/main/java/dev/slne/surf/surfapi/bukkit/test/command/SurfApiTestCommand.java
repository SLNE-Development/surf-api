package dev.slne.surf.api.paper.test.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.paper.test.command.subcommands.CommandExceptionTest;
import dev.slne.surf.api.paper.test.command.subcommands.GlowingTest;
import dev.slne.surf.api.paper.test.command.subcommands.InventoryTest;
import dev.slne.surf.api.paper.test.command.subcommands.MaxStacksizeTest;
import dev.slne.surf.api.paper.test.command.subcommands.PacketEntityTest;
import dev.slne.surf.api.paper.test.command.subcommands.PacketLoreTest;
import dev.slne.surf.api.paper.test.command.subcommands.PaginationTest;
import dev.slne.surf.api.paper.test.command.subcommands.PrefixConfigTest;
import dev.slne.surf.api.paper.test.command.subcommands.ReflectionTest;
import dev.slne.surf.api.paper.test.command.subcommands.ScoreboardTest;
import dev.slne.surf.api.paper.test.command.subcommands.SmoothTimeSkip;
import dev.slne.surf.api.paper.test.command.subcommands.SummonCommandTest;
import dev.slne.surf.api.paper.test.command.subcommands.SuspendCommandExecutionTest;
import dev.slne.surf.api.paper.test.command.subcommands.ToastTest;
import dev.slne.surf.api.paper.test.command.subcommands.VisualizerTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.display.DisplayTest;

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
            new MaxStacksizeTest("maxstacksize"),
            new VisualizerTest("visualizer"),
            new GlowingTest("glowing"),
            new PaginationTest("pagination"),
            new InventoryTest("inventory"),
            new ToastTest("toast"),
            new SuspendCommandExecutionTest("suspendCommandExecution"),
            new SummonCommandTest("summoncommand"),
            new DisplayTest("display")
        );
    }
}
