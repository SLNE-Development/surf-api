package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

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