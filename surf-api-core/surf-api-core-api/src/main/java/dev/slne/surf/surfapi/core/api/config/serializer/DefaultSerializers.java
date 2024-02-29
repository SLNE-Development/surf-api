package dev.slne.surf.surfapi.core.api.config.serializer;

import dev.slne.surf.surfapi.core.api.messages.Colors;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
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

    private static final MiniMessage.Builder builder = MiniMessage.builder()
        .editTags(builder -> {
          builder.tag("primary", colorTag(Colors.PRIMARY));
          builder.tag("secondary", colorTag(Colors.SECONDARY));
          builder.tag("info", colorTag(Colors.INFO));
          builder.tag("success", colorTag(Colors.SUCCESS));
          builder.tag("warning", colorTag(Colors.WARNING));
          builder.tag("error", colorTag(Colors.ERROR));
          builder.tag("variable_key", colorTag(Colors.VARIABLE_KEY));
          builder.tag("variable_value", colorTag(Colors.VARIABLE_VALUE));
          builder.tag("spacer", colorTag(Colors.SPACER));
          builder.tag("dark_spacer", colorTag(Colors.DARK_SPACER));
          builder.tag("prefix_color", colorTag(Colors.PREFIX_COLOR));
          builder.tag("prefix", Tag.selfClosingInserting(Colors.PREFIX));
        });
    private static MiniMessage miniMessage = builder.build();
    private static boolean modified = false;

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull Tag colorTag(TextColor color) {
      return Tag.styling(style -> style.color(color));
    }

    @Contract(pure = true)
    @Override
    public Class<Component> getTargetClass() {
      return Component.class;
    }

    @Override
    public @NotNull Component deserialise(@NotNull FlexibleType flexibleType)
        throws BadValueException {
      try {
        return getMiniMessage().deserialize(flexibleType.getString());
      } catch (ParsingException e) {
        throw flexibleType.badValueExceptionBuilder()
            .message("""
                Failed to parse component from string.
                Caused by: %s
                at: %s
                """.formatted(e.detailMessage(), e.endIndex()))
            .cause(e)
            .build();
      }
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
