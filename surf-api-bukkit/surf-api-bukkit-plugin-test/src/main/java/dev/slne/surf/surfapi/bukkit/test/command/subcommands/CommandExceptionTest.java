package dev.slne.surf.surfapi.bukkit.test.command.subcommands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.slne.surf.surfapi.core.api.command.SurfCommandUtil;
import net.kyori.adventure.text.Component;

public class CommandExceptionTest extends CommandAPICommand {

  public CommandExceptionTest(String commandName) {
    super(commandName);

    executes((commandSender, commandArguments) -> {
      SurfCommandUtil.failWithMessage(Component.text("This is a test fail"));
    });
  }
}
