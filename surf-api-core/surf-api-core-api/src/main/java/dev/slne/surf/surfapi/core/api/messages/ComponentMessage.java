package dev.slne.surf.surfapi.core.api.messages;

import static com.google.common.base.Preconditions.checkNotNull;

import com.mojang.brigadier.Message;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ComponentMessage implements Message, ComponentLike {

  @Internal
  public static final String COMPONENT_MESSAGE_PREFIX =
      "[ComponentMessage " + UUID.randomUUID() + "] ";

  private final ComponentLike message;

  @Contract(pure = true)
  public ComponentMessage(@NotNull ComponentLike message) {
    checkNotNull(message, "message");
    this.message = message;
  }

  @Override
  public String getString() {
    return COMPONENT_MESSAGE_PREFIX
        + GsonComponentSerializer.gson().serialize(message.asComponent());
  }

  @Override
  public @NotNull Component asComponent() {
    return message.asComponent();
  }
}
