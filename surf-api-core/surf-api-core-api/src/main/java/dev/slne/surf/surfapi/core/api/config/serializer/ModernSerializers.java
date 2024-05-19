package dev.slne.surf.surfapi.core.api.config.serializer;

import java.util.function.Consumer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

@Internal
@NonExtendable
public class ModernSerializers {

  public static Consumer<TypeSerializerCollection.Builder> SERIALIZERS = builder -> {

  };
}
