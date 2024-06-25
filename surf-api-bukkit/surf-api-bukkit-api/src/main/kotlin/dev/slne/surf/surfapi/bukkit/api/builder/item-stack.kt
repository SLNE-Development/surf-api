package dev.slne.surf.surfapi.bukkit.api.builder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class ItemMarker

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class MetaMarker

fun buildItem(
    material: Material,
    amount: Int = 1,
    init: (@ItemMarker ItemStack).() -> Unit
): ItemStack {
    val item = ItemStack(material, amount)
    item.init()
    return item
}

inline fun <reified M : ItemMeta> ItemStack.meta(crossinline init: (@MetaMarker M).() -> Unit) {
    editMeta(M::class.java) { it.init() }
}

@JvmName("meta0")
fun ItemStack.meta(init: (@MetaMarker ItemMeta).() -> Unit) {
    editMeta { it.init() }
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