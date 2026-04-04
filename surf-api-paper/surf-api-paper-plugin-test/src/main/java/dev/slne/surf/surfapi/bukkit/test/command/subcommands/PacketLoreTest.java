package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.paper.test.command.subcommands.lore.PacketLoreApply;
import dev.slne.surf.api.paper.test.command.subcommands.lore.PacketLoreCreate;

public class PacketLoreTest extends CommandAPICommand {

    public PacketLoreTest(String commandName) {
        super(commandName);

        withSubcommands(
            new PacketLoreCreate("create"),
            new PacketLoreApply("apply")
        );
    }
}
