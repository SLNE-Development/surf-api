package dev.slne.surf.surfapi.bukkit.server.reflection;

import dev.slne.surf.surfapi.core.api.reflection.annontation.Field;
import dev.slne.surf.surfapi.core.api.reflection.annontation.Field.Type;
import dev.slne.surf.surfapi.core.api.reflection.annontation.SurfProxy;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;

@SurfProxy(Item.class)
public interface ItemProxy {

  @Field(name = "components", type = Type.SETTER, overrideFinal = true)
  void setComponents(Item item, DataComponentMap components);
}
