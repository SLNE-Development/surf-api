package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

@DslMarker
annotation class SettingsDsl

@SettingsDsl
sealed class ViewSettingsBuilder {
    var font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT
    var headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT
    var cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK
    var cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG
    var cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP
    var cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP
    var navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE

    fun cancelAllInteractions() {
        cancelOnClick = true
        cancelOnDrag = true
        cancelOnDrop = true
        cancelOnPickup = true
    }

    fun allowAllInteractions() {
        cancelOnClick = false
        cancelOnDrag = false
        cancelOnDrop = false
        cancelOnPickup = false
    }
}

@SettingsDsl
class SimpleViewSettingsBuilder : ViewSettingsBuilder() {
    var rows: ViewRows = SurfViewSettingsDefaults.DEFAULT_VIEW_ROWS

    @PublishedApi
    internal fun build(): SimpleViewSettings = SimpleViewSettings(
        font = font,
        rows = rows,
        headerTextAlignment = headerTextAlignment,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
    )
}

@SettingsDsl
class PaginatedViewSettingsBuilder : ViewSettingsBuilder() {
    var paginationViewRows: PaginationViewRows = SurfViewSettingsDefaults.DEFAULT_PAGINATION_VIEW_ROWS
    var paginationButtonPosition: PaginationButtonPosition = SurfViewSettingsDefaults.DEFAULT_PAGINATION_BUTTON_POSITION

    @PublishedApi
    internal fun build(): PaginatedViewSettings = PaginatedViewSettings(
        font = font,
        headerTextAlignment = headerTextAlignment,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
        paginationViewRows = paginationViewRows,
        paginationButtonPosition = paginationButtonPosition
    )
}

inline fun simpleViewSettings(block: SimpleViewSettingsBuilder.() -> Unit = {}): SimpleViewSettings =
    SimpleViewSettingsBuilder().apply(block).build()

inline fun paginatedViewSettings(block: PaginatedViewSettingsBuilder.() -> Unit = {}): PaginatedViewSettings =
    PaginatedViewSettingsBuilder().apply(block).build()