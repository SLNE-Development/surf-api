package dev.slne.surf.api.gen.generator.reflection;

import dev.slne.surf.surfapi.core.api.reflection.Field;
import dev.slne.surf.surfapi.core.api.reflection.Field.Type;
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy;
import java.util.List;
import net.minecraft.core.RegistrySetBuilder;

@SurfProxy(RegistrySetBuilder.class)
public interface RegistrySetBuilderProxy {

  @Field(name = "entries", type = Type.GETTER)
  List<Object> getEntries(RegistrySetBuilder instance);
}
