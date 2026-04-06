package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.core.api.messages.Colors;
import net.kyori.adventure.text.Component;

public class PrefixConfigTest extends CommandAPICommand {

    public PrefixConfigTest(String commandName) {
        super(commandName);

        executes((sender, args) -> {
            sender.sendMessage(Colors.PREFIX.append(Component.text("Prefix Config Test")));
        });
    }
}
