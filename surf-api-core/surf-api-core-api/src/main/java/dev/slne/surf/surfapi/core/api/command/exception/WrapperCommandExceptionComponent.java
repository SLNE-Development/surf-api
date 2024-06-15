package dev.slne.surf.surfapi.core.api.command.exception;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.slne.surf.surfapi.core.api.messages.ComponentMessage;
import java.io.Serial;
import net.kyori.adventure.text.Component;

public class WrapperCommandExceptionComponent extends WrapperCommandSyntaxException {

  @Serial
  private static final long serialVersionUID = -5277462249842682916L;

  public WrapperCommandExceptionComponent(Component errorMessage) {
    super(new CommandSyntaxException(
        new SimpleCommandExceptionType(new ComponentMessage(errorMessage)),
        new ComponentMessage(errorMessage)));
  }

  @Override
  public String getMessage() {
    return getRawMessage().getString();
  }
}
