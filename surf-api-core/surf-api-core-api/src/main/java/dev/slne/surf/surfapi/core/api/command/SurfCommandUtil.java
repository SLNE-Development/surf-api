package dev.slne.surf.surfapi.core.api.command;

import static com.google.common.base.Preconditions.checkNotNull;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.slne.surf.surfapi.core.api.command.builder.CommandExceptionBuilder;
import dev.slne.surf.surfapi.core.api.command.exception.WrapperCommandExceptionComponent;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@NonExtendable
public interface SurfCommandUtil {

  @Contract("_ -> new")
  static @NotNull WrapperCommandExceptionComponent createException(ComponentLike message) {
    return new WrapperCommandExceptionComponent(checkNotNull(message, "message").asComponent());
  }

  @Contract("_ -> fail")
  static void failWithMessage(ComponentLike message) throws WrapperCommandSyntaxException {
    throw createException(message);
  }

  @Contract("_ -> fail")
  static void failWithBuilder(@NotNull CommandExceptionBuilder builder) throws WrapperCommandSyntaxException {
    throw createException(builder.build());
  }
}
