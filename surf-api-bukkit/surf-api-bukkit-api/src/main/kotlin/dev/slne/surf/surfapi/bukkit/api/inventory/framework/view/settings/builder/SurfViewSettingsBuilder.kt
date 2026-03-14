package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

@InventoryFramworkDSL
sealed class SurfViewSettingsBuilder {

    var font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT
        private set

    fun font(font: Key) {
        this.font = font
    }

    var headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT
        private set

    fun headerTextAlignment(alignment: TextAlignment) {
        this.headerTextAlignment = alignment
    }

    var cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK
        private set


    fun cancelOnClick(cancel: Boolean = true) {
        this.cancelOnClick = cancel
    }

    var cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG
        private set

    fun cancelOnDrag(cancel: Boolean = true) {
        this.cancelOnDrag = cancel
    }

    var cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP
        private set

    fun cancelOnDrop(cancel: Boolean = true) {
        this.cancelOnDrop = cancel
    }

    var cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP
        private set

    fun cancelOnPickup(cancel: Boolean = true) {
        this.cancelOnPickup = cancel
    }

    var navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE
        private set

    fun navigateBackOnOutsideClick(navigate: Boolean = true) {
        this.navigateBackOnOutsideClick = navigate
    }

    fun cancelAllInteractions() {
        cancelOnClick()
        cancelOnDrag()
        cancelOnDrop()
        cancelOnPickup()
    }

    @PublishedApi
    internal abstract fun build(): SurfViewSettings
}