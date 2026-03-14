package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFrameworkDSL
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align.TextAlignment
import net.kyori.adventure.key.Key

/**
 * Abstract base builder for all [SurfViewSettings] implementations.
 *
 * Provides shared DSL properties and mutation methods for the common settings shared by
 * [SimpleViewSettingsBuilder] and [PaginatedViewSettingsBuilder]. All properties start with
 * the defaults from [SurfViewSettingsDefaults] and can be overridden via their corresponding
 * setter functions.
 *
 * Instances are created by [simpleViewSettings] or [paginatedViewSettings]. Do not instantiate
 * directly.
 *
 * @see SimpleViewSettingsBuilder
 * @see PaginatedViewSettingsBuilder
 */
@InventoryFrameworkDSL
sealed class SurfViewSettingsBuilder {

    /**
     * The Adventure [Key] for the inventory title font.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_HEADER_FONT].
     */
    var font: Key = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT
        private set

    /**
     * Sets the font [Key] for the inventory title.
     *
     * @param font the Adventure [Key] of the resource-pack font
     */
    fun font(font: Key) {
        this.font = font
    }

    /**
     * The horizontal [TextAlignment] of the title in the header.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT].
     */
    var headerTextAlignment: TextAlignment = SurfViewSettingsDefaults.DEFAULT_HEADER_ALIGNMENT
        private set

    /**
     * Sets the [TextAlignment] for the inventory title.
     *
     * @param alignment the desired [TextAlignment]
     */
    fun headerTextAlignment(alignment: TextAlignment) {
        this.headerTextAlignment = alignment
    }

    /**
     * Whether click events in the inventory are cancelled by default.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK].
     */
    var cancelOnClick: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_CLICK
        private set

    /**
     * Sets whether click events should be cancelled.
     *
     * @param cancel `true` to cancel; defaults to `true`
     */
    fun cancelOnClick(cancel: Boolean = true) {
        this.cancelOnClick = cancel
    }

    /**
     * Whether drag events are cancelled by default.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG].
     */
    var cancelOnDrag: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DRAG
        private set

    /**
     * Sets whether drag events should be cancelled.
     *
     * @param cancel `true` to cancel; defaults to `true`
     */
    fun cancelOnDrag(cancel: Boolean = true) {
        this.cancelOnDrag = cancel
    }

    /**
     * Whether drop events are cancelled by default.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP].
     */
    var cancelOnDrop: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_DROP
        private set

    /**
     * Sets whether drop events should be cancelled.
     *
     * @param cancel `true` to cancel; defaults to `true`
     */
    fun cancelOnDrop(cancel: Boolean = true) {
        this.cancelOnDrop = cancel
    }

    /**
     * Whether pickup events are cancelled by default.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP].
     */
    var cancelOnPickup: Boolean = SurfViewSettingsDefaults.DEFAULT_CANCEL_ON_PICKUP
        private set

    /**
     * Sets whether pickup events should be cancelled.
     *
     * @param cancel `true` to cancel; defaults to `true`
     */
    fun cancelOnPickup(cancel: Boolean = true) {
        this.cancelOnPickup = cancel
    }

    /**
     * Whether clicking outside the inventory navigates back to the previous (parent) view.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE].
     */
    var navigateBackOnOutsideClick: Boolean = SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE
        private set

    /**
     * Sets whether an outside click should navigate back to the parent view.
     *
     * @param navigate `true` to navigate back; defaults to `true`
     */
    fun navigateBackOnOutsideClick(navigate: Boolean = true) {
        this.navigateBackOnOutsideClick = navigate
    }

    /**
     * Cancels all interaction types: click, drag, drop, and pickup.
     *
     * Equivalent to calling [cancelOnClick], [cancelOnDrag], [cancelOnDrop], and [cancelOnPickup].
     */
    fun cancelAllInteractions() {
        cancelOnClick()
        cancelOnDrag()
        cancelOnDrop()
        cancelOnPickup()
    }

    @PublishedApi
    internal abstract fun build(): SurfViewSettings
}