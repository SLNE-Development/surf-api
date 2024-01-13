package dev.slne.surf.surfapi.bukkit.test.command;

import dev.jorel.commandapi.CommandAPICommand;
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
                new ReflectionTest("reflection")
        );
    }
}
