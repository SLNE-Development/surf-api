package dev.slne.surf.api.gen.generator;

import dev.slne.surf.api.gen.generator.types.GeneratedKeyType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;


public interface Generators {

  SourceGenerator[] CORE_API_GENERATORS = new SourceGenerator[]{
      simpleKey("SoundKeys", Registries.SOUND_EVENT)
  };

  private static <T> SourceGenerator simpleKey(final String className,
      final ResourceKey<? extends Registry<T>> registryKey) {
    return new GeneratedKeyType<>(className, "dev.slne.surf.surfapi.core.api.generated", registryKey
    );
  }
}
