package dev.slne.surf.api.core.inventory.framework.internal

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the invariants the header layout rests on.
 *
 * `ViewContainer` resets the render cursor with `-(textureWidth + positionalShift)` after every
 * component, so a width that does not match the pixels a component actually advances the cursor by
 * shifts everything rendered after it. For text drawn in the vanilla font that width comes from
 * [DefaultFontWidths]; the shift that centers it comes from [TextAlignmentMath]; the shift that
 * puts a texture or a text run on a specific slot column comes from [ViewSlotGeometry].
 */
class ViewHeaderLayoutTest {

    private val titles = listOf(
        "",
        "A",
        "Shop",
        "Spieler-Statistiken",
        "iiiiiiiii",
        "WWWWWWWWW",
        "Straße",
        "Menü 1/4",
        "!!!...:::",
        "🎉 Event 🎉",
    )

    @Test
    fun `vanilla widths are measured per code point`() {
        // Two code points, one of which is a surrogate pair — four UTF-16 chars in total.
        assertEquals(2 * DefaultFontWidths.DEFAULT_ADVANCE, DefaultFontWidths.textWidth("A🎉"))
    }

    @Test
    fun `narrow glyphs are measured narrower than regular ones`() {
        assertEquals(2, DefaultFontWidths.charWidth('i'.code))
        assertEquals(3, DefaultFontWidths.charWidth('l'.code))
        assertEquals(4, DefaultFontWidths.charWidth(' '.code))
        assertEquals(7, DefaultFontWidths.charWidth('@'.code))
        assertEquals(DefaultFontWidths.DEFAULT_ADVANCE, DefaultFontWidths.charWidth('W'.code))
        assertEquals(DefaultFontWidths.DEFAULT_ADVANCE, DefaultFontWidths.charWidth('ß'.code))
    }

    @Test
    fun `bold adds one pixel per glyph`() {
        val text = "Shop"
        assertEquals(
            DefaultFontWidths.textWidth(text) + text.length,
            DefaultFontWidths.textWidth(text, bold = true)
        )
    }

    @Test
    fun `overrides win over the built-in table`() {
        val overrides = Int2IntOpenHashMap(1).apply { put('i'.code, 9) }

        assertEquals(9, DefaultFontWidths.charWidth('i'.code, overrides = overrides))
        assertEquals(18, DefaultFontWidths.textWidth("ii", overrides = overrides))
    }

    @Test
    fun `text width equals the sum of the glyph advances`() {
        for (title in titles) {
            var expected = 0
            var index = 0
            while (index < title.length) {
                val codePoint = title.codePointAt(index)
                expected += DefaultFontWidths.charWidth(codePoint)
                index += Character.charCount(codePoint)
            }

            assertEquals(expected, DefaultFontWidths.textWidth(title), "width of '$title'")
        }
    }

    @Test
    fun `centered titles stay centred whatever they contain`() {
        // Geometry of the vanilla chest inventory, see the platform ViewHeaderGeometry defaults.
        val leftShift = -8
        val padding = 0
        val containerWidth = 176

        for (title in titles) {
            val width = DefaultFontWidths.textWidth(title)
            val shift =
                TextAlignmentMath.centerAlignedShift(width, leftShift, padding, containerWidth)

            // Doubled to compare half-pixel centres in integer math.
            val textCentre = (2 * shift) + width
            val containerCentre = (2 * leftShift) + containerWidth

            assertTrue(
                (textCentre - containerCentre) in -2..2,
                "'$title' is off centre by ${(textCentre - containerCentre) / 2.0}px"
            )
        }
    }

    @Test
    fun `right aligned titles end at the same pixel`() {
        val leftShift = -8
        val padding = 0
        val containerWidth = 176

        val ends = titles.map { title ->
            val width = DefaultFontWidths.textWidth(title)
            TextAlignmentMath.rightAlignedShift(
                width,
                leftShift,
                padding,
                containerWidth
            ) + width
        }

        assertEquals(1, ends.distinct().size, "right aligned titles end at $ends")
    }

    @Test
    fun `a component is measured across its whole tree`() {
        val component = Component.text("Guthaben: ").append(Component.text("1.250"))

        assertEquals(DefaultFontWidths.textWidth("Guthaben: 1.250"), DefaultFontWidths.componentWidth(component))
    }

    @Test
    fun `colour does not change a measured width`() {
        val plain = Component.text("Shop")
        val coloured = Component.text("Shop").color(NamedTextColor.GOLD)

        assertEquals(
            DefaultFontWidths.componentWidth(plain),
            DefaultFontWidths.componentWidth(coloured)
        )
    }

    @Test
    fun `bold is measured per part and inherited by children`() {
        val boldChild = Component.text("A")
            .append(Component.text("B").decorate(TextDecoration.BOLD))
        val boldParent = Component.text("A").decorate(TextDecoration.BOLD)
            .append(Component.text("B"))
        val boldParentPlainChild = Component.text("A").decorate(TextDecoration.BOLD)
            .append(Component.text("B").decoration(TextDecoration.BOLD, false))

        val plain = DefaultFontWidths.charWidth('A'.code)

        assertEquals((2 * plain) + 1, DefaultFontWidths.componentWidth(boldChild))
        assertEquals((2 * plain) + 2, DefaultFontWidths.componentWidth(boldParent))
        assertEquals((2 * plain) + 1, DefaultFontWidths.componentWidth(boldParentPlainChild))
    }

    @Test
    fun `translatables are measured on their fallback`() {
        val withFallback = Component.translatable("surf.menu.title", "Shop")
        val withoutFallback = Component.translatable("shop")

        assertEquals(DefaultFontWidths.textWidth("Shop"), DefaultFontWidths.componentWidth(withFallback))
        assertEquals(DefaultFontWidths.textWidth("shop"), DefaultFontWidths.componentWidth(withoutFallback))
    }

    /** Reverse of [ShiftGlyphs.renderShift]: maps each single-glyph shift back to its pixel amount. */
    private val shiftAmounts: Map<Char, Int> = buildMap {
        for (exponent in 0..9) {
            val amount = 1 shl exponent
            for (signed in intArrayOf(amount, -amount)) {
                val glyph = ShiftGlyphs.renderShift(signed)
                check(glyph.length == 1) { "expected a single glyph for $signed, got '$glyph'" }
                put(glyph[0], signed)
            }
        }
    }

    /**
     * Independently measures how far a laid-out component advances the cursor: shift glyphs
     * contribute their own amount, every other code point the advance of the font it is measured in.
     */
    private fun advanceOf(component: Component, charSize: Int, bold: Boolean = false): Int {
        val effectiveBold = when (component.decoration(TextDecoration.BOLD)) {
            TextDecoration.State.TRUE -> true
            TextDecoration.State.FALSE -> false
            TextDecoration.State.NOT_SET -> bold
        }
        val glyphSize = charSize + if (effectiveBold) 1 else 0

        var advance = 0
        val text = (component as? TextComponent)?.content() ?: ""
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val shift = if (Character.charCount(codePoint) == 1) {
                shiftAmounts[codePoint.toChar()]
            } else {
                null
            }

            advance += shift ?: glyphSize
            index += Character.charCount(codePoint)
        }

        for (child in component.children()) {
            advance += advanceOf(child, charSize, effectiveBold)
        }

        return advance
    }

    /** Metrics of the fixed-width, capitals-only resource-pack menu font. */
    private fun menuLayout(component: Component) = layoutViewText(
        component = component,
        charSpacing = -1,
        uppercase = true,
        spaceShift = 4,
    ) { _, bold -> if (bold) 10 else 9 }

    @Test
    fun `a fixed width font reports the advance it actually renders`() {
        val components = listOf(
            Component.text("Shop"),
            Component.text("Menu 1/4"),
            Component.text("A").append(Component.text("BC").decorate(TextDecoration.BOLD)),
            Component.text("Guthaben: ").append(Component.text("1250")),
            Component.text(""),
        )

        for (component in components) {
            val laidOut = menuLayout(component)

            assertEquals(
                advanceOf(laidOut.component, charSize = 9),
                laidOut.width,
                "reported width of '" + plain(component) + "'"
            )
        }
    }

    @Test
    fun `a font without a space glyph renders spaces as shifts`() {
        val text = "AB CD"
        val laidOut = layoutViewText(
            component = Component.text(text),
            charSpacing = 0,
            uppercase = false,
            spaceShift = 4,
        ) { _, _ -> 9 }

        // Four glyphs at 9px plus a 4px shift, and the space itself must be gone: it would render
        // as the missing-glyph box of the font, on the title line instead of in the row.
        assertEquals((4 * 9) + 4, laidOut.width)
        assertEquals(laidOut.width, advanceOf(laidOut.component, charSize = 9))
        assertTrue(
            ' ' !in (laidOut.component as TextComponent).content(),
            "the space was emitted verbatim: '${(laidOut.component as TextComponent).content()}'"
        )
    }

    @Test
    fun `a font with a space glyph keeps the space`() {
        val laidOut = layoutViewText(
            component = Component.text("AB CD"),
            charSpacing = 0,
            uppercase = false,
            spaceShift = null,
        ) { _, _ -> 9 }

        assertEquals(5 * 9, laidOut.width)
        assertEquals("AB CD", (laidOut.component as TextComponent).content())
    }

    @Test
    fun `the menu font renders eight pixels per glyph`() {
        // 9px cell tightened by 1px of spacing in front of every glyph.
        assertEquals(4 * 8, menuLayout(Component.text("Shop")).width)
    }

    @Test
    fun `a capitals only font uppercases every part`() {
        val laidOut = menuLayout(Component.text("Shop ").append(Component.text("Seite")))

        // Shift glyphs filtered out, which for these metrics includes the space: it is rendered as
        // a shift because the font draws no space glyph.
        assertEquals("SHOPSEITE", plain(laidOut.component).filter { it !in shiftAmounts })
    }

    @Test
    fun `uppercasing that changes the glyph count is measured on the result`() {
        // 'ß' uppercases to 'SS', so the run is one glyph wider than the input.
        assertEquals(2 * 8, menuLayout(Component.text("ß")).width)
    }

    @Test
    fun `translatable parts are neither uppercased nor spaced`() {
        val laidOut = menuLayout(Component.translatable("surf.menu.title", "Shop"))

        assertEquals("Shop", plain(laidOut.component))
        assertEquals(4 * 9, laidOut.width)
    }

    private fun plain(component: Component): String = buildString {
        append((component as? TextComponent)?.content() ?: fallbackOf(component))
        for (child in component.children()) append(plain(child))
    }

    private fun fallbackOf(component: Component) =
        (component as? TranslatableComponent)?.let { it.fallback() ?: it.key() } ?: ""

    @Test
    fun `column shifts follow the slot pitch`() {
        val originX = -1

        assertEquals(-1, ViewSlotGeometry.columnShift(0, originX))
        assertEquals(17, ViewSlotGeometry.columnShift(1, originX))
        assertEquals(143, ViewSlotGeometry.columnShift(8, originX))
        assertEquals(162, ViewSlotGeometry.spanWidth(ViewSlotGeometry.COLUMNS))
    }

    @Test
    fun `the vanilla slot grid is centred in the vanilla inventory`() {
        // Both centres must coincide, otherwise a centred title would not sit above a centred row
        // text. Doubled to compare half-pixel centres in integer math.
        val inventoryCentre = (2 * -8) + 176
        val gridCentre = (2 * ViewSlotGeometry.columnShift(0, -1)) +
                ViewSlotGeometry.spanWidth(ViewSlotGeometry.COLUMNS)

        assertEquals(inventoryCentre, gridCentre)
    }

    @Test
    fun `slot indices map to the column and one-based row they render in`() {
        assertEquals(0, ViewSlotGeometry.columnOf(0))
        assertEquals(1, ViewSlotGeometry.rowOf(0))
        assertEquals(8, ViewSlotGeometry.columnOf(8))
        assertEquals(1, ViewSlotGeometry.rowOf(8))
        assertEquals(0, ViewSlotGeometry.columnOf(9))
        assertEquals(2, ViewSlotGeometry.rowOf(9))
        assertEquals(4, ViewSlotGeometry.columnOf(31))
        assertEquals(4, ViewSlotGeometry.rowOf(31))

        for (slot in 0 until ViewSlotGeometry.COLUMNS * ViewSlotGeometry.MAX_ROWS) {
            assertEquals(
                slot,
                ViewSlotGeometry.slotOf(
                    ViewSlotGeometry.columnOf(slot),
                    ViewSlotGeometry.rowOf(slot)
                ),
                "round-trip of slot $slot"
            )
        }
    }

    @Test
    fun `a text run centred in a column span stays inside it`() {
        val originX = -1
        val columns = 2..6
        val spanShift = ViewSlotGeometry.columnShift(columns.first, originX)
        val spanWidth = ViewSlotGeometry.spanWidth(columns.count())

        for (title in titles) {
            val width = DefaultFontWidths.textWidth(title)
            if (width > spanWidth) continue

            val shift = TextAlignmentMath.centerAlignedShift(width, spanShift, 0, spanWidth)

            assertTrue(shift >= spanShift, "'$title' starts left of the span")
            assertTrue(shift + width <= spanShift + spanWidth + 1, "'$title' ends right of the span")
        }
    }
}
