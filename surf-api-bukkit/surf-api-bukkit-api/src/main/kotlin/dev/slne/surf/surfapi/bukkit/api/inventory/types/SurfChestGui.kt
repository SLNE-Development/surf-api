package dev.slne.surf.surfapi.bukkit.api.inventory.types

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.SinglePlayerGui
import dev.slne.surf.surfapi.bukkit.api.inventory.SurfGui
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.MenuMarker
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.Range

/**
 * A chest-type [SurfGui] backed by an inventory-framework [ChestGui].
 *
 * On construction, all click and drag events on both the top and bottom inventories
 * are cancelled by default, preventing players from moving items unless explicitly
 * allowed by an item's click handler. Rows must be in the range `2..6`.
 *
 * Instances are usually created via [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.menu]
 * or [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.childMenu].
 *
 * ```kotlin
 * val gui = menu(Component.text("My Chest"), rows = 4) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(4, 0), ItemStack(Material.NETHER_STAR)) {
 *             click = { isCancelled = true }
 *         }
 *     }
 * }
 * gui.gui.show(player)
 * ```
 *
 * @param title the Adventure [Component] title displayed in the inventory header
 * @param rows the number of rows in the chest inventory (must be in `2..6`)
 * @param parent the parent [SurfGui] to return to when [SurfGui.backToParent] is called,
 *   or `null` if this is a root menu
 * @throws IllegalStateException if [rows] is not in the range `2..6`
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.menu
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.childMenu
 * @see SurfChestSinglePlayerGui
 */
@MenuMarker
open class SurfChestGui internal constructor(
    title: Component,
    rows: @Range(from = 2, to = 6) Int = 6,
    override val parent: SurfGui? = null
) :
    ChestGui(rows, ComponentHolder.of(title)), SurfGui {
    override val gui: NamedGui
        get() = this

    init {
        check(rows in 2..6) { "Rows must be between 2 and 6" }

        this.setOnBottomClick { event -> event.isCancelled = true }
        this.setOnBottomDrag { event -> event.isCancelled = true }
        this.setOnTopClick { event -> event.isCancelled = true }
        this.setOnTopDrag { event -> event.isCancelled = true }
    }
}

/**
 * A [SurfChestGui] bound to a specific [Player], combining chest-menu behaviour
 * with [SinglePlayerGui] so that [open] can be called without providing a player reference.
 *
 * Instances are typically created via
 * [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.playerMenu] or
 * [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.childPlayerMenu].
 *
 * ```kotlin
 * val gui = playerMenu(Component.text("Player Profile"), player, rows = 3) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(4, 0)) { click = { isCancelled = true } }
 *     }
 * }
 * gui.open()
 * ```
 *
 * @param title the Adventure [Component] title for the inventory header
 * @param player the [Player] this GUI is bound to
 * @param rows the number of rows in the chest (must be in `2..6`)
 * @param parent the parent [SurfGui], or `null` if this is a root menu
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.playerMenu
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.childPlayerMenu
 * @see SurfChestGui
 */
@MenuMarker
class SurfChestSinglePlayerGui internal constructor(
    title: Component,
    override val player: Player,
    rows: @Range(from = 2, to = 6) Int = 6,
    override val parent: SurfGui? = null,
) :
    SurfChestGui(title, rows, parent), SinglePlayerGui