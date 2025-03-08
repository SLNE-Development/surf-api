package dev.slne.surf.surfapi.bukkit.api.builder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class ItemMarker

inline fun buildItem(
    material: Material,
    amount: Int = 1,
    init: (@ItemMarker ItemStack).() -> Unit,
): ItemStack {
    val item = ItemStack(material, amount)
    item.init()
    return item
}

inline fun <reified M : ItemMeta> ItemStack.meta(block: (@ItemMarker M).() -> Unit): Boolean {
    val meta = itemMeta as? M ?: return false
    meta.block()
    itemMeta = meta
    return true
}

@JvmName("meta0")
inline fun ItemStack.meta(init: (@ItemMarker ItemMeta).() -> Unit) {
    meta<ItemMeta>(init)
}

fun ItemStack.displayName(name: Component) {
    meta {
        displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
    }
}

fun ItemStack.lore(vararg lore: Component) {
    meta {
        lore(lore.map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
    }
}