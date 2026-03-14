package dev.slne.surf.surfapi.bukkit.api.inventory.item

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.SinglePlayerGui
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * A configurable [GuiItem] extension used within [SurfGui] pane builders.
 *
 * [SurfGuiItem] augments a plain [GuiItem] with:
 * - A [click] handler that is registered as the item's inventory click action.
 * - An optional [itemPermission] that, for [SinglePlayerGui]s, gates whether the item
 *   is displayed based on the viewer's permissions.
 * - A [condition] predicate that determines whether the item should be added to the pane at all.
 *
 * Instances are created and configured inside a [SurfGui.StaticPane.item] DSL block:
 *
 * ```kotlin
 * staticPane(slot(0, 0), height = 1) {
 *     item(slot(4, 0), ItemStack(Material.EMERALD)) {
 *         condition = { player.hasPermission("myPlugin.emerald") }
 *         click = {
 *             isCancelled = true
 *             player.sendMessage("You clicked the emerald!")
 *         }
 *     }
 * }
 * ```
 *
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.SurfGui.item
 * @see SinglePlayerGui
 */
class SurfGuiItem : GuiItem {

    /**
     * Constructs a [SurfGuiItem] backed by the given [ItemStack].
     * If [item] is `null`, an empty [ItemStack] is used instead.
     *
     * @param item the base [ItemStack] to display, or `null` for an empty item
     */
    constructor(item: ItemStack?) : super(item ?: ItemStack.empty())

    /**
     * Constructs a [SurfGuiItem] backed by an empty [ItemStack].
     */
    constructor() : super(ItemStack.empty())

    /**
     * The click handler invoked when a player clicks this item.
     *
     * Setting this property internally calls [GuiItem.setAction] so the handler is
     * registered with the inventory framework.
     */
    var click: InventoryClickEvent.() -> Unit = {}
        set(value) = setAction(value)

    /**
     * An optional Bukkit permission string that gates whether this item is shown.
     *
     * When non-null and this item is used inside a [SinglePlayerGui], the item is only
     * added to the pane if the bound player has the specified permission.
     *
     * Set this property via the [permission] helper inside a [SinglePlayerGui] scope.
     *
     * @see permission
     */
    var itemPermission: String? = null
        private set

    /**
     * A predicate that controls whether this item should be added to the pane.
     *
     * Defaults to `{ true }`. If the predicate returns `false`, the item is skipped entirely.
     *
     * ```kotlin
     * item(slot(0, 0)) {
     *     condition = { serverHasFeature }
     * }
     * ```
     */
    var condition: () -> Boolean = { true }

    /**
     * Sets the [itemPermission] required to see this item when inside a [SinglePlayerGui].
     *
     * This function must be called from within a [SinglePlayerGui] DSL scope.
     *
     * ```kotlin
     * item(slot(0, 0)) {
     *     permission("myPlugin.admin")
     * }
     * ```
     *
     * @receiver the [SinglePlayerGui] that provides the player context
     * @param permission the Bukkit permission node the player must have
     */
    fun SinglePlayerGui.permission(permission: String) {
        itemPermission = permission
    }
}