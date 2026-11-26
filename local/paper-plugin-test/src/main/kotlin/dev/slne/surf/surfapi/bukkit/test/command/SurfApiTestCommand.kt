package dev.slne.surf.surfapi.bukkit.test.command

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.*

class SurfApiTestCommand : CommandAPICommand("surfapitest") {
    init {
        withPermission("surfapitest.use")
        withSubcommands(
            PacketLoreTest("packetlore"),
            SidebarTest("sidebar"),
            SmoothTimeSkip("smoothtimeskip"),
            ReflectionTest("reflection"),
            PrefixConfigTest("prefixconfig"),
            CommandExceptionTest("commandexception"),
            MaxStacksizeTest("maxstacksize"),
            VisualizerTest("visualizer"),
            GlowingTest("glowing"),
            PaginationTest("pagination"),
            InventoryTest("inventory"),
            ItemInsertionTest("insertion"),
            ItemRemovalTest("removal"),
            ToastTest("toast"),
            SuspendCommandExecutionTest("suspendCommandExecution"),
            SummonCommandTest("summoncommand"),
            SurfEventHandlerTest("eventhandler"),
            ShowItemCommand("showitem"),
            SortInvCommand("sortInv"),
            StableActionbarTestCommand("stableActionbarTest"),
            SignedMessageArgumentTest("signedmessage"),
            BlockPdcContainerTest("blockpdc"),
            OfflineInventoryEditTest("editOfflineInventory"),
            ModernSerializerTestConfigCommand("modernSerializerTestConfig"),
            SuspendRequirementTestCommand("suspendRequirement"),
        )
    }
}
