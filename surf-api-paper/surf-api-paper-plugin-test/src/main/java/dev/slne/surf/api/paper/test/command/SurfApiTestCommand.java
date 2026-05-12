package dev.slne.surf.api.paper.test.command;

import dev.jorel.commandapi.*;
import dev.slne.surf.api.paper.test.command.subcommands.*;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.*;

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
                new SurfEventHandlerTest("eventhandler"),
                new ShowItemCommand("showitem"),
                new SortInvCommand("sortInv"),
                new SignedMessageArgumentTest("signedmessage"),
                new BlockPdcContainerTest("blockpdc"),
                new OfflineInventoryEditTest("editOfflineInventory")
        );
    }
}
