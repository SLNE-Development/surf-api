package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

/**
 * Sealed interface that defines the full configuration contract for all Surf view types.
 *
 * Concrete implementations are [SimpleViewSettings] (for non-paginated views) and
 * [PaginatedViewSettings] (for paginated views). All properties have sensible defaults
 * defined in [SurfViewSettingsDefaults].
 *
 * @property font the Adventure [Key] of the resource-pack font for the inventory title
 * @property headerTextAlignment horizontal [TextAlignment] of the title in the header
 * @property cancelOnClick whether inventory click events are cancelled by default
 * @property cancelOnDrag whether inventory drag events are cancelled by default
 * @property cancelOnDrop whether item-drop events are cancelled by default
 * @property cancelOnPickup whether item-pickup events are cancelled by default
 * @property navigateBackOnOutsideClick whether clicking outside the inventory navigates
 *   back to the previous (parent) view
 * @property rows the number of chest rows displayed in the inventory
 * @see SimpleViewSettings
 * @see PaginatedViewSettings
 * @see SurfViewSettingsDefaults
 */
sealed interface SurfViewSettings {
    val font: Key
    val headerTextAlignment: TextAlignment
    val cancelOnClick: Boolean
    val cancelOnDrag: Boolean
    val cancelOnDrop: Boolean
    val cancelOnPickup: Boolean
    val navigateBackOnOutsideClick: Boolean
    val rows: ViewRows
}