package dev.slne.surf.surfapi.bukkit.api.inventory

import org.bukkit.entity.Player

/**
 * A [SurfGui] bound to a single [Player].
 *
 * Extends [SurfGui] with a [player] property and a convenience [open] function
 * that immediately shows the GUI to the bound player.
 *
 * Instances are typically created via
 * [dev.slne.surf.surfapi.bukkit.api.inventory.dsl.playerMenu] or
 * [dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestSinglePlayerGui].
 *
 * ```kotlin
 * val gui = playerMenu(Component.text("Profile"), player) {
 *     staticPane(slot(0, 0), height = 1) {
 *         item(slot(4, 0)) {
 *             click = { isCancelled = true }
 *         }
 *     }
 * }
 * gui.open()
 * ```
 *
 * @see SurfGui
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.dsl.playerMenu
 */
interface SinglePlayerGui : SurfGui {
    /**
     * The [Player] this GUI is bound to.
     */
    val player: Player

    /**
     * Shows this GUI to the bound [player].
     *
     * Equivalent to calling `gui.show(player)`.
     */
    fun open() = gui.show(player)
}