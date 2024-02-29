package dev.slne.surf.surfapi.velocity.api.messages;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Moved to {@link dev.slne.surf.surfapi.core.api.command.builder.CommandExceptionBuilder}
 */
@ScheduledForRemoval(inVersion = "1.1.0")
@Deprecated(forRemoval = true, since = "1.0.0")
public class CommandExceptionBuilder extends
    dev.slne.surf.surfapi.core.api.command.builder.CommandExceptionBuilder {

  public CommandExceptionBuilder(@Nullable String detailErrorMessage, String input, int cursor) {
    super(detailErrorMessage, input, cursor);
  }

  public Component build() {
    return super.build();
  }

  public Component build(@Nullable Component prefix) {
    return super.build(prefix);
  }

  protected @Nullable Component getContext() {
    return super.getContext();
  }
}
