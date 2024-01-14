package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.Reflection;

public class ReflectionTest extends CommandAPICommand {

    public ReflectionTest(String commandName) {
        super(commandName);

        executes((commandSender, commandArguments) -> {
           Reflection.RESTART_COMMAND.restart();
        });
    }
}
