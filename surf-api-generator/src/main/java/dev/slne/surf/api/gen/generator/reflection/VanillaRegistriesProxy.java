package dev.slne.surf.api.gen.generator.reflection;

import dev.slne.surf.surfapi.core.api.reflection.Field;
import dev.slne.surf.surfapi.core.api.reflection.Static;
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.registries.VanillaRegistries;

@SurfProxy(VanillaRegistries.class)
public interface VanillaRegistriesProxy {

  @Field(name = "BUILDER", type = Field.Type.GETTER)
  @Static
  RegistrySetBuilder getBUILDER();
}
