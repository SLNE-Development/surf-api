package dev.slne.surf.api.minestom.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * DSL marker for the Minestom item builder DSL.
 *
 * Mirrors the Paper `@ItemDsl` marker annotation
 * (`dev.slne.surf.api.paper.builder.ItemDsl`). On Paper the marker is applied to the
 * `ItemStack` receiver type directly because Bukkit item stacks are mutable; Minestom item
 * stacks are immutable, so the receiver is the [ItemDsl] builder class instead and this marker
 * is applied to that class.
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@DslMarker
annotation class ItemDslMarker

/**
 * Mutable builder receiver used by [buildItem].
 *
 * Wraps a Minestom [ItemStack.Builder] and exposes the same surface the Paper item DSL exposes
 * on `org.bukkit.inventory.ItemStack`, so consumer code reads identically on both platforms:
 *
 * ```kotlin
 * withItem(Material.DIAMOND) {
 *     displayName { text("Diamond") }
 * }
 * ```
 *
 * @param material the material the item is built from
 * @param amount the initial stack size
 */
@ItemDslMarker
class ItemDsl(material: Material, amount: Int = 1) {

    /**
     * The underlying Minestom builder.
     *
     * Public escape hatch for everything this DSL does not wrap (arbitrary
     * `net.minestom.server.component.DataComponents` entries, tags, ...). Also required to be
     * public because [buildItem] is `inline`.
     */
    val builder: ItemStack.Builder = ItemStack.builder(material).amount(amount)

    /**
     * Sets the stack size of the item.
     *
     * @param amount the new stack size
     */
    fun amount(amount: Int) {
        builder.amount(amount)
    }

    /**
     * Sets the display name of the item.
     *
     * @param name the display name component
     */
    fun displayName(name: Component) {
        builder.set(
            DataComponents.CUSTOM_NAME,
            name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        )
    }

    /**
     * Sets the display name of the item using a [SurfComponentBuilder] DSL block.
     *
     * ```kotlin
     * displayName { text("Diamond") }
     * ```
     *
     * @param block DSL block used to construct the display name
     */
    inline fun displayName(block: @ItemDslMarker SurfComponentBuilder.() -> Unit) {
        displayName(SurfComponentBuilder().apply(block).build())
    }

    /**
     * Sets the lore of the item.
     *
     * @param lore the lore lines
     */
    fun lore(vararg lore: Component) {
        builder.set(
            DataComponents.LORE,
            lore.map { it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) }
        )
    }

    /**
     * Sets a single lore line using a [SurfComponentBuilder] DSL block.
     *
     * @param block DSL block used to construct the lore line
     */
    fun lore(block: @ItemDslMarker SurfComponentBuilder.() -> Unit) {
        lore(SurfComponentBuilder().apply(block).build())
    }

    /**
     * Sets the item model of the item.
     *
     * @param model the item model key, e.g. `key("nexo", "surf_menu_icon_...")`
     */
    fun itemModel(model: Key) {
        itemModel(model.asString())
    }

    /**
     * Sets the item model of the item.
     *
     * @param model the item model identifier, e.g. `"nexo:surf_menu_icon_..."`
     */
    fun itemModel(model: String) {
        builder.set(DataComponents.ITEM_MODEL, model)
    }

    /**
     * Builds the immutable [ItemStack].
     */
    fun build(): ItemStack = builder.build()
}

/**
 * Builds a Minestom [ItemStack] from [material] using the [ItemDsl] DSL.
 *
 * ```kotlin
 * buildItem(Material.DIAMOND) {
 *     displayName { text("Diamond") }
 * }
 * ```
 *
 * @param material the material to build the item from
 * @param amount the stack size
 * @param init DSL block applied to the [ItemDsl] receiver
 * @return the built [ItemStack]
 */
inline fun buildItem(
    material: Material,
    amount: Int = 1,
    init: (@ItemDslMarker ItemDsl).() -> Unit = {},
): ItemStack = ItemDsl(material, amount).apply(init).build()
