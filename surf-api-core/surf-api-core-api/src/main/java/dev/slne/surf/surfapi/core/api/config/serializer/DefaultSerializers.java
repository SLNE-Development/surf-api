package dev.slne.surf.surfapi.core.api.config.serializer;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;

/**
 * These are the default serializers that are always enabled when creating a configuration via
 * {@link dev.slne.surf.surfapi.core.api.config.SurfConfigManager#create(Class, Path, String)}
 */
@ApiStatus.NonExtendable
public final class DefaultSerializers {

  public static final List<ValueSerialiser<?>> DEFAULTS = List.of(
      new ComponentSerializer()
  );

  public static final class ComponentSerializer implements ValueSerialiser<Component> {

    private static final MiniMessage.Builder builder = MiniMessage.builder();
    private static MiniMessage miniMessage = builder.build();
    private static boolean modified = false;

    @Contract(pure = true)
    @Override
    public Class<Component> getTargetClass() {
      return Component.class;
    }

    @Override
    public @NotNull Component deserialise(@NotNull FlexibleType flexibleType) throws BadValueException {
      return getMiniMessage().deserialize(flexibleType.getString());
    }

    @Override
    public @NotNull String serialise(Component value, Decomposer decomposer) {
      return getMiniMessage().serialize(value);
    }

    private static MiniMessage getMiniMessage() {
      if (modified) {
        miniMessage = builder.build();
        modified = false;
      }

      return miniMessage;
    }

    public static void customizeMiniMessage(@NotNull Consumer<MiniMessage.Builder> modifier) {
      modifier.accept(builder);
      modified = true;
    }
  }
}
