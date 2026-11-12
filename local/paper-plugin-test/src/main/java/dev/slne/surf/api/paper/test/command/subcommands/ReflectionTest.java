package dev.slne.surf.api.paper.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.api.paper.test.command.subcommands.reflection.Reflection;

public class ReflectionTest extends CommandAPICommand {

    public ReflectionTest(String commandName) {
        super(commandName);

        executes((commandSender, commandArguments) -> {
            Reflection.RESTART_COMMAND.restart();
        });
    }
}
