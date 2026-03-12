package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

data class PaginatedViewSettings(
    override val font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT,
    override val headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT,
    override val cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK,
    override val cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG,
    override val cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP,
    override val cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP,
    override val navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE,
    val paginationViewRows: PaginationViewRows = SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS,
    val paginationButtonPosition: PaginationButtonPosition = SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION
) : SurfViewSettings {
    override val rows: ViewRows = paginationViewRows.actualRows
    internal val paginationButtonRow = if (paginationButtonPosition == PaginationButtonPosition.BOTTOM) {
        rows.rows
    } else {
        1
    }
}