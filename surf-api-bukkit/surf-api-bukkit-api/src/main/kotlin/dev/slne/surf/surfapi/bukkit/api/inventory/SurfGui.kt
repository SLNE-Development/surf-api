package dev.slne.surf.surfapi.bukkit.api.inventory

import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.PaneMarker
import dev.slne.surf.surfapi.bukkit.api.inventory.item.SurfGuiItem
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * Base interface for all Surf GUI menus.
 *
 * A [SurfGui] wraps an inventory-framework [NamedGui] and optionally holds
 * a reference to a [parent] menu so that nested menus can navigate back
 * through their parent chain.
 *
 * Implementations are typically created via the DSL builder functions
 * [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.menu] or
 * [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.playerMenu].
 *
 * ```kotlin
 * val gui = menu(Component.text("My Menu")) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(0, 0), ItemStack(Material.DIAMOND)) {
 *             click = { isCancelled = true }
 *         }
 *     }
 * }
 * gui.gui.show(player)
 * ```
 *
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.menu
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.playerMenu
 * @see SinglePlayerGui
 */
interface SurfGui {
    /**
     * The parent [SurfGui] that this GUI was opened from, or `null` if this is a root menu.
     */
    val parent: SurfGui?

    /**
     * The underlying inventory-framework [NamedGui] that represents the actual inventory.
     */
    val gui: NamedGui

    /**
     * Navigates the [HumanEntity] back to the [parent] menu.
     *
     * If [parent] is non-null, the parent GUI is shown and updated on the next server tick.
     * If [parent] is `null`, the inventory is closed with reason [InventoryCloseEvent.Reason.PLUGIN].
     *
     * The navigation is deferred by one tick to avoid Bukkit concurrency issues.
     *
     * ```kotlin
     * item(slot(8, 0)) {
     *     click = { whoClicked.backToParent() }
     * }
     * ```
     *
     * @receiver the [HumanEntity] that should navigate back
     */
    fun HumanEntity.backToParent() {
        server.scheduler.runTaskLater(JavaPlugin.getProvidingPlugin(SurfGui::class.java), Runnable {
            if (parent != null) {
                val gui = parent!!.gui
                gui.show(this)
                gui.update()
            } else {
                closeInventory(InventoryCloseEvent.Reason.PLUGIN)
            }
        }, 1L)
    }

    /**
     * Returns the full ancestry chain starting from this GUI up to the root.
     *
     * The first element of the returned list is always `this`, the second is its [parent],
     * and so on. The last element is the root GUI (i.e. the one whose [parent] is `null`).
     *
     * @return an ordered list of all [SurfGui]s in the parent chain, including `this`
     */
    fun walkParents(): List<SurfGui> = generateSequence(this) { it.parent }.toList()

    /**
     * Adds a [SurfGuiItem] to this [StaticPane] at the given [slot].
     *
     * The [init] lambda is called on a freshly created [SurfGuiItem] to configure it.
     * The item is only added to the pane if:
     * - Its [SurfGuiItem.condition] returns `true`
     * - The viewer (when this GUI is a [SinglePlayerGui]) has the required
     *   [SurfGuiItem.itemPermission], if one was set
     *
     * ```kotlin
     * staticPane(slot(0, 0), height = 1) {
     *     item(slot(0, 0), ItemStack(Material.DIAMOND)) {
     *         click = { isCancelled = true }
     *         condition = { someCondition }
     *     }
     * }
     * ```
     *
     * @receiver the [StaticPane] to which the item should be added
     * @param slot the [Slot] position within the pane
     * @param item the base [ItemStack] to display; defaults to an empty item if `null`
     * @param init configuration block applied to the [SurfGuiItem]
     * @see SurfGuiItem
     */
    fun StaticPane.item(
        slot: Slot,
        item: ItemStack? = null,
        init: (@PaneMarker SurfGuiItem).() -> Unit,
    ) {
        val guiItem = SurfGuiItem(item)
        guiItem.init()

        if (!guiItem.condition()) {
            return
        }

        if (this@SurfGui is SinglePlayerGui) {
            if (guiItem.itemPermission?.let { player.hasPermission(it) } == false) {
                return
            }
        }

        addItem(guiItem, slot)
    }
}