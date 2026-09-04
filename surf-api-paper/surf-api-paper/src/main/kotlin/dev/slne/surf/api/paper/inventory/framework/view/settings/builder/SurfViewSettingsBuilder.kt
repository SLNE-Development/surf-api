package dev.slne.surf.api.paper.inventory.framework.view.settings.builder

import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.paper.inventory.framework.view.InventoryFrameworkDSL
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewFontMetrics
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewHeaderGeometry
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor

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
     * Colour for header text that does not carry a colour of its own.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR].
     */
    var headerTextColor: TextColor = SurfViewSettingsDefaults.DEFAULT_HEADER_TEXT_COLOR
        private set

    /**
     * Sets the colour used for header text that does not carry one of its own.
     *
     * @param color the colour to fall back to
     */
    fun headerTextColor(color: TextColor) {
        this.headerTextColor = color
    }

    /**
     * Glyph metrics of the font the title is rendered in.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_HEADER_FONT_METRICS].
     */
    var headerFontMetrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_HEADER_FONT_METRICS
        private set

    /**
     * Sets the glyph metrics of the title font.
     *
     * Has to match the font set through [font], otherwise the title is measured wrong and lands off
     * centre.
     *
     * @param metrics the metrics of the title font
     */
    fun headerFontMetrics(metrics: ViewFontMetrics) {
        this.headerFontMetrics = metrics
    }

    /**
     * Glyph metrics of the fonts row text is rendered in.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS].
     */
    var rowFontMetrics: ViewFontMetrics = SurfViewSettingsDefaults.DEFAULT_ROW_FONT_METRICS
        private set

    /**
     * Sets the glyph metrics of the row fonts.
     *
     * Has to match the fonts set through [rowFont], otherwise row text is measured wrong and lands
     * off centre. Pass [ViewFontMetrics.VANILLA] for row fonts that are copies of the client font.
     *
     * @param metrics the metrics of the row fonts
     */
    fun rowFontMetrics(metrics: ViewFontMetrics) {
        this.rowFontMetrics = metrics
    }

    /**
     * Whether the per-row background glyph of a custom inventory texture is rendered.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_BACKGROUND_GLYPH].
     */
    var backgroundGlyph: Boolean = SurfViewSettingsDefaults.DEFAULT_BACKGROUND_GLYPH
        private set

    /**
     * Sets whether the view draws its own inventory background through the per-row glyphs of
     * [ViewContainerGlyphComponent][dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerGlyphComponent].
     *
     * Leave it off for a view rendering on the vanilla inventory texture. When enabling it, also
     * re-measure [headerGeometry] for the custom texture.
     *
     * @param render `true` to render the background glyph; defaults to `true`
     */
    fun backgroundGlyph(render: Boolean = true) {
        this.backgroundGlyph = render
    }

    /**
     * The [ViewHeaderGeometry] describing the pixel layout of the inventory.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY].
     */
    var headerGeometry: ViewHeaderGeometry = SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY
        private set

    /**
     * Sets the [ViewHeaderGeometry] of the header.
     *
     * @param geometry the geometry of the header texture in use
     */
    fun headerGeometry(geometry: ViewHeaderGeometry) {
        this.headerGeometry = geometry
    }

    /**
     * Derives the [ViewHeaderGeometry] of the header from the current one.
     *
     * ```kotlin
     * settings {
     *     headerGeometry { copy(titleWidth = 240) }
     * }
     * ```
     *
     * @param block returns the geometry to use, with the current one as its receiver
     */
    fun headerGeometry(block: ViewHeaderGeometry.() -> ViewHeaderGeometry) {
        this.headerGeometry = headerGeometry.block()
    }

    /**
     * The fonts that render header text on a slot row, keyed by one-based row number.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_ROW_FONTS].
     */
    var rowFonts: Int2ObjectMap<Key> = SurfViewSettingsDefaults.DEFAULT_ROW_FONTS
        private set

    /**
     * Replaces the whole row-font mapping.
     *
     * Rows without an entry fall back to [SurfViewSettingsDefaults.rowFont].
     *
     * @param fonts the fonts to use, keyed by one-based row number
     */
    fun rowFonts(fonts: Int2ObjectMap<Key>) {
        this.rowFonts = fonts
    }

    /**
     * Overrides the font that renders header text on a single slot [row].
     *
     * Each row needs its own font because a glyph's vertical position comes from the `ascent` its
     * font provider declares in the resource pack — see [SurfViewSettingsDefaults.rowFont].
     *
     * @param row the one-based slot row (1-6)
     * @param font the Adventure [Key] of the font ascended onto that row
     */
    fun rowFont(row: Int, font: Key) {
        val fonts = Int2ObjectOpenHashMap<Key>(rowFonts)
        fonts.put(row, font)
        this.rowFonts = fonts.freeze()
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
    var navigateBackOnOutsideClick: Boolean =
        SurfViewSettingsDefaults.DEFAULT_NAVIGATE_BACK_ON_CLOSE
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