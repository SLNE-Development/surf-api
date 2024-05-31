package dev.slne.surf.surfapi.core.api.config.serializer;

import dev.slne.surf.surfapi.core.api.config.serializer.DefaultSerializers.ComponentSerializer;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.AbstractListChildSerializer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.CheckedConsumer;

@Internal
@NonExtendable
public class ModernSerializers {

  public static Consumer<TypeSerializerCollection.Builder> SERIALIZERS = builder -> {
    builder.register(Component.class, new ComponentSerializer());
    builder.register(LinkedListSerializer.TYPE, new LinkedListSerializer());
  };

  public static class ComponentSerializer implements TypeSerializer<Component> {

    @Override
    public Component deserialize(Type type, ConfigurationNode node) throws SerializationException {
      final String miniMessage = node.getString();

      if (miniMessage == null) {
        return Component.empty();
      }

      return DefaultSerializers.ComponentSerializer.getMiniMessage().deserialize(miniMessage);
    }

    @Override
    public void serialize(Type type, @Nullable Component obj, ConfigurationNode node)
        throws SerializationException {

      if (obj == null) {
        return;
      }

      node.set(DefaultSerializers.ComponentSerializer.getMiniMessage().serialize(obj));
    }
  }

  public static class LinkedListSerializer extends AbstractListChildSerializer<LinkedList<?>> {
    static final TypeToken<LinkedList<?>> TYPE = new TypeToken<LinkedList<?>>() {};

    @Override
    protected Type elementType(Type containerType) throws SerializationException {
      if (containerType instanceof AnnotatedParameterizedType) {
        AnnotatedType[] annotatedTypes = ((AnnotatedParameterizedType) containerType)
            .getAnnotatedActualTypeArguments();
        if (annotatedTypes.length > 0) {
          return annotatedTypes[0].getType();
        }
      }
      return Object.class;
    }

    @Override
    protected LinkedList<?> createNew(int length, Type elementType) throws SerializationException {
      return new LinkedList<>();
    }

    @Override
    protected void forEachElement(LinkedList<?> collection,
        CheckedConsumer<Object, SerializationException> action) throws SerializationException {
      for (Object element : collection) {
        action.accept(element);
      }
    }

    @Override
    protected void deserializeSingle(int index, LinkedList<?> collection,
        @Nullable Object deserialized) throws SerializationException {
      ((LinkedList) collection).add(deserialized);
    }
  }
}
