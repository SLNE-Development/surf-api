package dev.slne.surf.api.gen.generator.reflection;

import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;

public class Reflection {

  public static final VanillaRegistriesProxy VANILLA_REGISTRIES;
  public static final RegistrySetBuilderProxy REGISTRY_SET_BUILDER;
  public static final RegistryStubProxy REGISTRY_STUB;

  static {
    final SurfReflection reflection = SurfReflection.getInstance();

    VANILLA_REGISTRIES = reflection.createProxy(VanillaRegistriesProxy.class);
    REGISTRY_SET_BUILDER = reflection.createProxy(RegistrySetBuilderProxy.class);
    REGISTRY_STUB = reflection.createProxy(RegistryStubProxy.class);
  }

  private Reflection() {
    throw new UnsupportedOperationException("This class cannot be instantiated");
  }
}
