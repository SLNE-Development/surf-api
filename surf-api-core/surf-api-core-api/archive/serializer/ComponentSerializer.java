package dev.slne.surf.surfapi.core.api.config.serializer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.configurate.serialize.ScalarSerializer;

import java.lang.reflect.Type;
import java.util.function.Predicate;

public class ComponentSerializer extends ScalarSerializer<Component> {

  public ComponentSerializer() {
    super(Component.class);
  }

  @Override
  public Component deserialize(Type type, Object obj) {
    return MiniMessage.miniMessage().deserialize(obj.toString());
  }

  @Override
  protected Object serialize(Component item, Predicate<Class<?>> typeSupported) {
    return MiniMessage.miniMessage().serialize(item);
  }
}
