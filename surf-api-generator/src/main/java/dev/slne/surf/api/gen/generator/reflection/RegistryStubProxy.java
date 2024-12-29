package dev.slne.surf.api.gen.generator.reflection;

import dev.slne.surf.surfapi.core.api.reflection.Name;
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder.RegistryBootstrap;
import net.minecraft.resources.ResourceKey;

@SurfProxy(qualifiedName = "net.minecraft.core.RegistrySetBuilder.RegistryStub")
public interface RegistryStubProxy {

  @Name("key")
  ResourceKey<? extends Registry<?>> key(Object instance);

  @Name("bootstrap")
  RegistryBootstrap<?> bootstrap(Object instance);
}
