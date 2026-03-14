package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SimpleViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.ViewRows

@InventoryFramworkDSL
class SimpleViewSettingsBuilder @PublishedApi internal constructor() : SurfViewSettingsBuilder() {
    var rows: ViewRows = SurfViewSettingsDefaults.DEFAULT_VIEW_ROWS
        private set

    fun rows(rows: ViewRows) {
        this.rows = rows
    }

    fun rows(@ViewRows.Companion.Rows count: Int) {
        this.rows = ViewRows.byRows(count)
    }

    @PublishedApi
    override fun build(): SimpleViewSettings = SimpleViewSettings(
        font = font,
        headerTextAlignment = headerTextAlignment,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
        rows = rows,
    )
}

inline fun simpleViewSettings(block: SimpleViewSettingsBuilder.() -> Unit): SimpleViewSettings =
    SimpleViewSettingsBuilder().apply(block).build()