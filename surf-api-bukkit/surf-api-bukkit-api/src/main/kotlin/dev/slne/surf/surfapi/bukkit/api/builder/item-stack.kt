package dev.slne.surf.surfapi.bukkit.api.builder

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class ItemDsl

inline fun buildItem(
    material: Material,
    amount: Int = 1,
    init: (@ItemDsl ItemStack).() -> Unit,
): ItemStack {
    val item = ItemStack(material, amount)
    item.init()
    return item
}

inline fun ItemStack(
    material: Material,
    amount: Int = 1,
    init: (@ItemDsl ItemStack).() -> Unit,
): ItemStack {
    return buildItem(material, amount, init)
}

inline fun <reified M : ItemMeta> ItemStack.meta(block: (@ItemDsl M).() -> Unit): Boolean {
    val meta = itemMeta as? M ?: return false
    meta.block()
    itemMeta = meta
    return true
}

@JvmName("meta0")
inline fun ItemStack.meta(init: (@ItemDsl ItemMeta).() -> Unit) {
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

fun ItemStack.lore(block: @ItemDsl SurfComponentBuilder.() -> Unit) {
    lore(SurfComponentBuilder().apply(block).build())
}

fun ItemStack.buildLore(block: @ItemDsl LoreBuilder.() -> Unit) {
    val loreBuilder = LoreBuilder().apply(block)
    lore(loreBuilder.build())
}

@ItemDsl
class LoreBuilder {
    private val lore = mutableObjectListOf<Component>()

    fun line(block: @ItemDsl SurfComponentBuilder.() -> Unit) {
        lore.add(SurfComponentBuilder().apply(block).build())
    }

    operator fun Component.unaryPlus() {
        lore.add(this)
    }

    internal fun build() =
        lore.map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
}