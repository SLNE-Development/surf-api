package dev.slne.surf.api.paper.inventory.framework.view.settings

import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

/**
 * View settings for a simple (non-paginated) [AbstractSurfView].
 *
 * Create instances via the DSL builder [simpleViewSettings] or through
 * the [SimpleViewSettingsBuilder] within a `settings { }` DSL block.
 *
 * @property font the Adventure [Key] of the font used for the inventory title
 * @property headerTextAlignment horizontal alignment of the title text
 * @property cancelOnClick whether click events should be cancelled by default
 * @property cancelOnDrag whether drag events should be cancelled by default
 * @property cancelOnDrop whether drop events should be cancelled by default
 * @property cancelOnPickup whether pickup events should be cancelled by default
 * @property navigateBackOnOutsideClick whether an outside click navigates to the parent view
 * @property rows the number of rows displayed in the inventory
 * @see SurfViewSettings
 * @see SimpleViewSettingsBuilder
 */
data class SimpleViewSettings(
    override val font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT,
    override val headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT,
    override val cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK,
    override val cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG,
    override val cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP,
    override val cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP,
    override val navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE,
    override val rows: ViewRows = SurfViewSettingsDefaults.DEFAULT_VIEW_ROWS,
) : SurfViewSettings
