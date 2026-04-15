package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import net.minecraft.core.component.DataComponentMap
import net.minecraft.world.item.Item
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Proxies(Item::class)
interface V26_1ItemProxy {
    @FieldSetter("components")
    fun setComponents(item: Item, components: DataComponentMap)
}