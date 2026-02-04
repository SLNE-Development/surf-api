package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component

data class ResolvedSlot(
    val component: Component,
    val guiItem: GuiItem?,
) {
    override fun toString(): String {
        return "ResolvedSlot(component=$component, guiItem=$guiItem)"
    }
}
