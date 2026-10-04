package dev.slne.surf.api.minestom.inventory.framework.view.icon

import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.minestom.builder.ItemDsl
import dev.slne.surf.api.minestom.builder.buildItem
import net.minestom.server.item.Material

fun viewIcon(
    icon: ViewIconType,
    color: ViewIconColor,
    init: ItemDsl.() -> Unit = {}
) = ViewIcon(icon, color).build(init)

class ViewIcon(
    val icon: ViewIconType,
    val color: ViewIconColor,
) {
    val itemModel: String
        get() = "surf_menu_icon_${color.configName}_${icon.configName}"

    @Suppress("UnstableApiUsage")
    fun build(init: ItemDsl.() -> Unit) = buildItem(Material.PAPER) {
        init()

        itemModel(key("nexo", itemModel))
    }
}
