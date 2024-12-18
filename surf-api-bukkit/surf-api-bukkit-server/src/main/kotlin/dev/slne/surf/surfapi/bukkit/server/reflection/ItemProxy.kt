package dev.slne.surf.surfapi.bukkit.server.reflection

import dev.slne.surf.surfapi.core.api.reflection.Field
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import net.minecraft.core.component.DataComponentMap
import net.minecraft.world.item.Item

@SurfProxy(Item::class)
interface ItemProxy {
    @Field("components", Field.Type.SETTER, overrideFinal = true)
    fun setComponents(item: Item, components: DataComponentMap)
}
