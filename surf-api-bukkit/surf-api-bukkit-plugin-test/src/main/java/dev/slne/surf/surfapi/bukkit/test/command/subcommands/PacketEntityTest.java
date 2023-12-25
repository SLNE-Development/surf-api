package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity.CreateHardcoded;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity.CreatePacketEntity;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity.RemovePacketEntity;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity.ShowHidePacketEntity;

public class PacketEntityTest extends CommandAPICommand {
    public PacketEntityTest(String commandName) {
        super(commandName);

        withSubcommands(
                new CreatePacketEntity("create"),
                new ShowHidePacketEntity("showhide"),
                new RemovePacketEntity("remove"),
                new CreateHardcoded("hardcoded")
        );
    }
}
