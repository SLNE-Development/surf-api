package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import net.minecraft.core.component.DataComponentMap
import net.minecraft.world.item.Item
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Suppress("ClassName")
@Proxies(Item::class)
interface V1_21_11ItemProxy {
    @FieldSetter("components")
    fun setComponents(item: Item, components: DataComponentMap)
}