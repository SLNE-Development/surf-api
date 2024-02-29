package dev.slne.surf.surfapi.bukkit.test.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.CommandExceptionTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.PacketEntityTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.PacketLoreTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.PrefixConfigTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.ReflectionTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.ScoreboardTest;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.SmoothTimeSkip;

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
        new CommandExceptionTest("commandexception")
    );
  }
}
