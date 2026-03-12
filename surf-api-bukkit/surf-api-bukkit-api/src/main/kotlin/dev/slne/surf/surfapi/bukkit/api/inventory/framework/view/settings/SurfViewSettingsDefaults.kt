package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import dev.slne.surf.surfapi.core.api.messages.adventure.key

object SurfViewSettingsDefaults {
    const val DEFAULT_CANCEL_ON_CLICK = true
    const val DEFAULT_CANCEL_ON_DRAG = true
    const val DEFAULT_CANCEL_ON_DROP = true
    const val DEFAULT_CANCEL_ON_PICKUP = true
    const val DEFAULT_NAVIGATE_BACK_ON_CLOSE = true

    val DEFAULT_HEADER_FONT = key("surf", "menu_font_normal_ascended")
    val DEFAULT_MENU_FONT = key("surf", "menu")
    val DEFAULT_HEADER_ALIGNMENT = TextAlignment.CENTER
    val DEFAULT_VIEW_ROWS = ViewRows.FIVE
    val DEFAULT_PAGINATION_VIEW_ROWS = PaginationViewRows.FOUR
    val DEFAULT_PAGINATION_BUTTON_POSITION = PaginationButtonPosition.BOTTOM
}