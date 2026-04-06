package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.core.api.command.SurfCommandUtil;
import net.kyori.adventure.text.Component;

public class CommandExceptionTest extends CommandAPICommand {

    public CommandExceptionTest(String commandName) {
        super(commandName);

        executes((commandSender, commandArguments) -> {
            SurfCommandUtil.failWithMessage(Component.text("This is a test fail"));
        });
    }
}
