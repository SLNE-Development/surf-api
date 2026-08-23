package dev.slne.surf.api.paper.inventory.framework.view.settings

import dev.slne.surf.api.core.inventory.framework.internal.ViewSlotGeometry
import dev.slne.surf.api.core.messages.adventure.key
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.paper.inventory.framework.view.pagination.PaginationPageIndicator
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.TextColor

/**
 * Centralised default values for all [SurfViewSettings] properties.
 *
 * These constants and properties are referenced by [SimpleViewSettings],
 * [PaginatedViewSettings], and the corresponding builder classes to ensure
 * consistent defaults across all view types.
 *
 * All defaults can be overridden per-view via the `settings { }` DSL block.
 *
 * @see SurfViewSettings
 * @see dev.slne.surf.api.paper.inventory.framework.view.settings.builder.SurfViewSettingsBuilder
 */
object SurfViewSettingsDefaults {
    /** Default value for [SurfViewSettings.cancelOnClick]: `true`. */
    const val DEFAULT_CANCEL_ON_CLICK = true

    /** Default value for [SurfViewSettings.cancelOnDrag]: `true`. */
    const val DEFAULT_CANCEL_ON_DRAG = true

    /** Default value for [SurfViewSettings.cancelOnDrop]: `true`. */
    const val DEFAULT_CANCEL_ON_DROP = true

    /** Default value for [SurfViewSettings.cancelOnPickup]: `true`. */
    const val DEFAULT_CANCEL_ON_PICKUP = true

    /** Default value for [SurfViewSettings.navigateBackOnOutsideClick]: `true`. */
    const val DEFAULT_NAVIGATE_BACK_ON_CLOSE = true

    /**
     * The Adventure [Key] for the default header font: the client's own font
     * (`minecraft:default`).
     *
     * The title is rendered like any other vanilla inventory title — plain, in the player's font,
     * at the top of the header. Its horizontal position is computed from the proportional glyph
     * widths of that font, see
     * [DefaultFontWidths][dev.slne.surf.api.core.inventory.framework.internal.DefaultFontWidths].
     */
    val DEFAULT_HEADER_FONT = key("minecraft", "default")

    /**
     * The Adventure [Key] of the ascended copy of the vanilla font (`surf:menu_font_normal_ascended`).
     *
     * Renders text one line below the title row. Pass it to
     * [font][dev.slne.surf.api.paper.inventory.framework.view.settings.builder.SurfViewSettingsBuilder.font]
     * for a view whose header text should sit lower than the vanilla title position.
     */
    val ASCENDED_HEADER_FONT = key("surf", "menu_font_normal_ascended")

    /** The Adventure [Key] for the default menu font (`surf:menu`), used for the header textures. */
    val DEFAULT_MENU_FONT = key("surf", "menu")

    /** Default [TextAlignment] for the header: [TextAlignment.CENTER]. */
    val DEFAULT_HEADER_ALIGNMENT = TextAlignment.CENTER

    /**
     * Default colour for header text that does not carry one of its own: `#404040`, the colour
     * vanilla draws inventory titles in.
     *
     * Both the title and any row text are drawn on the vanilla inventory background, which is light
     * grey — white text would be all but invisible on it. Pass a coloured component (or set
     * [SurfViewSettings.headerTextColor]) for anything else.
     */
    val DEFAULT_HEADER_TEXT_COLOR: TextColor = TextColor.color(0x404040)

    /**
     * Default value for [SurfViewSettings.backgroundGlyph]: `false`.
     *
     * Views render on the vanilla inventory background. Set it to `true` for a resource pack that
     * draws its own inventory texture through the per-row glyphs of
     * [ViewContainerGlyphComponent][dev.slne.surf.api.paper.inventory.framework.view.container.component.components.ViewContainerGlyphComponent],
     * and re-measure [ViewHeaderGeometry] for that texture.
     */
    const val DEFAULT_BACKGROUND_GLYPH = false

    /** Default [ViewHeaderGeometry]: the pixel layout of the current header texture. */
    val DEFAULT_HEADER_GEOMETRY = ViewHeaderGeometry.DEFAULT

    /**
     * Default [ViewFontMetrics] for the inventory title: [ViewFontMetrics.VANILLA], matching the
     * client font of [DEFAULT_HEADER_FONT].
     */
    val DEFAULT_HEADER_FONT_METRICS = ViewFontMetrics.VANILLA

    /**
     * Default [ViewFontMetrics] for text rendered on a slot row: [ViewFontMetrics.VANILLA_ROW],
     * because the row fonts of [rowFont] are copies of the client font glyph sheets, ascended onto
     * their row - same widths as the client font, but without its space provider.
     *
     * Switch it to [ViewFontMetrics.SURF_MENU] for row fonts built from the fixed-width,
     * capitals-only menu font texture instead.
     */
    val DEFAULT_ROW_FONT_METRICS = ViewFontMetrics.VANILLA_ROW

    /** Default [TextAlignment] for text rendered on a slot row: [TextAlignment.CENTER]. */
    val DEFAULT_ROW_TEXT_ALIGNMENT = TextAlignment.CENTER

    /** Default column span for text rendered on a slot row: the full width of the slot grid. */
    val DEFAULT_ROW_TEXT_COLUMNS = 0 until ViewSlotGeometry.COLUMNS

    /** Default number of rows for simple views: [ViewRows.FIVE]. */
    val DEFAULT_VIEW_ROWS = ViewRows.FIVE

    /** Default [PaginationViewRows] for paginated views: [PaginationViewRows.FIVE]. */
    val DEFAULT_PAGINATION_VIEW_ROWS = PaginationViewRows.FIVE

    /**
     * Default [PaginationEmptyRows] for paginated views: [PaginationEmptyRows.NONE], so the
     * pagination content fills every row except the navigation button row.
     */
    val DEFAULT_PAGINATION_EMPTY_ROWS = PaginationEmptyRows.NONE

    /** Default [PaginationButtonPosition] for paginated views: [PaginationButtonPosition.BOTTOM]. */
    val DEFAULT_PAGINATION_BUTTON_POSITION = PaginationButtonPosition.BOTTOM

    /**
     * Default [PaginationPageIndicator] for paginated views: [PaginationPageIndicator.Default],
     * which renders `current/total` between the navigation buttons.
     */
    val DEFAULT_PAGINATION_PAGE_INDICATOR: PaginationPageIndicator = PaginationPageIndicator.Default

    /**
     * Default [Sound] played to the viewer when a paginated view switches to another page:
     * `minecraft:item.book.page_turn`, the sound the client plays when a book page is turned.
     */
    val DEFAULT_PAGINATION_SWITCH_SOUND: Sound = Sound.sound(
        key("minecraft", "item.book.page_turn"),
        Sound.Source.PLAYER,
        1f,
        1f
    )

    /**
     * Returns the default font that renders header text on the one-based slot [row]:
     * `surf:menu_font_row_1` … `surf:menu_font_row_6`.
     *
     * A glyph's vertical position is baked into the `ascent` of its font provider in the resource
     * pack, so text can only be moved down a row by rendering it in a font that declares the
     * matching `ascent`. Each of these fonts is therefore a copy of the vanilla font whose `ascent`
     * puts its glyphs in the vertical centre of the row - not above its slots.
     *
     * @param row the one-based slot row (1–6)
     */
    fun rowFont(@ViewRows.Companion.Rows row: Int): Key = key("surf", "menu_font_row_$row")

    /**
     * Default [SurfViewSettings.rowFonts]: the [rowFont] of every slot row a chest inventory can
     * have, keyed by its one-based row number.
     */
    val DEFAULT_ROW_FONTS: Int2ObjectMap<Key> =
        Int2ObjectOpenHashMap<Key>(ViewSlotGeometry.MAX_ROWS).apply {
            for (row in 1..ViewSlotGeometry.MAX_ROWS) {
                put(row, rowFont(row))
            }
        }.freeze()
}
