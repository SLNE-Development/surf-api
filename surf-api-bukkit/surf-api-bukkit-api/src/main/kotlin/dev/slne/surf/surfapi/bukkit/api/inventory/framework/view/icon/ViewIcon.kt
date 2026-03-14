package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.icon

import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

fun viewIcon(
    icon: ViewIconType,
    color: ViewIconColor,
    init: ItemStack.() -> Unit = {}
) = ViewIcon(icon, color).build(init)

class ViewIcon(
    val icon: ViewIconType,
    val color: ViewIconColor,
) {
    val itemModel: String
        get() = "surf_menu_icon_${color.configName}_${icon.configName}"

    @Suppress("UnstableApiUsage")
    fun build(init: ItemStack.() -> Unit) = buildItem(ItemType.PAPER) {
        init()

        setData(DataComponentTypes.ITEM_MODEL, key("nexo", itemModel))
    }
}
