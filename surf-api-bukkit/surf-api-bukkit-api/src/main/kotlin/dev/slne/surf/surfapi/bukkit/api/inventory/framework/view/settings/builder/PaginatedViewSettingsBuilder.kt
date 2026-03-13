package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.SurfViewDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginatedViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginationButtonPosition
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettingsDefaults

@SurfViewDsl
class PaginatedViewSettingsBuilder @PublishedApi internal constructor() : SurfViewSettingsBuilder() {

    var paginationViewRows: PaginationViewRows = SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS
        private set

    fun paginationViewRows(rows: PaginationViewRows) {
        this.paginationViewRows = rows
    }

    var paginationButtonPosition: PaginationButtonPosition = SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION
        private set

    fun paginationButtonPosition(position: PaginationButtonPosition) {
        this.paginationButtonPosition = position
    }

    fun paginationButtonsAtBottom() {
        paginationButtonPosition(PaginationButtonPosition.BOTTOM)
    }

    fun paginationButtonsAtTop() {
        paginationButtonPosition(PaginationButtonPosition.TOP)
    }

    @PublishedApi
    override fun build(): PaginatedViewSettings = PaginatedViewSettings(
        font = font,
        headerTextAlignment = headerTextAlignment,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
        paginationViewRows = paginationViewRows,
        paginationButtonPosition = paginationButtonPosition,
    )
}

inline fun paginatedViewSettings(block: PaginatedViewSettingsBuilder.() -> Unit): PaginatedViewSettings =
    PaginatedViewSettingsBuilder().apply(block).build()