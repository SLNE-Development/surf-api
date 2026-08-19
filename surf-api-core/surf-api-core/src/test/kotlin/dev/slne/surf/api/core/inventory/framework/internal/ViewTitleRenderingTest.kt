package dev.slne.surf.api.core.inventory.framework.internal

import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntMaps
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the invariant the whole header layout rests on: the width [renderViewTitle] reports must
 * equal the number of pixels the string it returns actually advances the render cursor by.
 *
 * `ViewContainer` resets the cursor with `-(textureWidth + positionalShift)` after every component,
 * so any mismatch here shifts every texture that follows the title.
 */
class ViewTitleRenderingTest {

    private val charSize = 9
    private val charSpacing = -1

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
     * Independently measures how far [text] advances the cursor: shift glyphs contribute their own
     * amount, every other code point contributes its glyph width.
     */
    private fun advanceOf(text: String, charWidths: Int2IntMap = Int2IntMaps.EMPTY_MAP): Int {
        var advance = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val shift = if (Character.charCount(codePoint) == 1) {
                shiftAmounts[codePoint.toChar()]
            } else {
                null
            }

            advance += shift
                ?: if (charWidths.containsKey(codePoint)) charWidths.get(codePoint) else charSize
            index += Character.charCount(codePoint)
        }

        return advance
    }

    private val titles = listOf(
        "",
        "A",
        "Shop",
        "Straße",
        "Grüße",
        "Äpfel",
        "Öl",
        "Menü",
        "Ein sehr langer Titel für das Menü",
        "Werkzeug & Rüstung",
        "Statistiken 🎉",
        "ẞ",
    )

    @Test
    fun `reported width matches the rendered advance`() {
        for (title in titles) {
            for (alignRight in listOf(false, true)) {
                val rendered = renderViewTitle(title, charSize, charSpacing, alignRight)

                assertEquals(
                    advanceOf(rendered.text),
                    rendered.width,
                    "width mismatch for '$title' (alignRight=$alignRight)"
                )
            }
        }
    }

    @Test
    fun `reported width matches the rendered advance with per-glyph overrides`() {
        val charWidths = Int2IntOpenHashMap().apply {
            put('Ä'.code, 11)
            put('Ü'.code, 11)
            put('I'.code, 4)
            put(' '.code, 4)
        }

        for (title in titles) {
            for (alignRight in listOf(false, true)) {
                val rendered = renderViewTitle(title, charSize, charSpacing, alignRight, charWidths)

                assertEquals(
                    advanceOf(rendered.text, charWidths),
                    rendered.width,
                    "width mismatch for '$title' (alignRight=$alignRight)"
                )
            }
        }
    }

    @Test
    fun `eszett is measured as the two glyphs it uppercases to`() {
        // "Straße" renders as STRASSE: 7 glyphs, 6 gaps, plus the leading spacing glyph.
        val rendered = renderViewTitle("Straße", charSize, charSpacing, alignRight = false)

        assertEquals((7 * charSize) + (6 * charSpacing) + charSpacing, rendered.width)
    }

    @Test
    fun `right aligned titles omit the leading spacing glyph`() {
        val left = renderViewTitle("Straße", charSize, charSpacing, alignRight = false)
        val right = renderViewTitle("Straße", charSize, charSpacing, alignRight = true)

        assertEquals(left.width - charSpacing, right.width)
    }

    @Test
    fun `empty titles render nothing`() {
        val rendered = renderViewTitle("", charSize, charSpacing, alignRight = false)

        assertEquals("", rendered.text)
        assertEquals(0, rendered.width)
    }

    @Test
    fun `surrogate pairs count as a single glyph`() {
        val rendered = renderViewTitle("🎉", charSize, charSpacing, alignRight = true)

        assertEquals(charSize, rendered.width)
    }

    @Test
    fun `text width is measured per code point`() {
        // Two code points, one of which is a surrogate pair — four UTF-16 chars in total.
        assertEquals(
            (2 * charSize) + charSpacing,
            TextAlignmentMath.textWidth("A🎉", charSize, charSpacing)
        )
    }

    @Test
    fun `centered titles stay centred whatever they contain`() {
        // Geometry of the real header container, see ViewContainerTitleComponent.
        val leftShift = 31
        val padding = 2
        val containerWidth = 100

        for (title in titles) {
            val rendered = renderViewTitle(title, charSize, charSpacing, alignRight = false)
            val shift = TextAlignmentMath.centerAlignedShift(
                rendered.width,
                leftShift,
                padding,
                containerWidth
            )

            // Doubled to compare half-pixel centres in integer math.
            val textCentre = (2 * shift) + rendered.width
            val containerCentre = (2 * leftShift) + containerWidth

            assertTrue(
                (textCentre - containerCentre) in -2..2,
                "'$title' is off centre by ${(textCentre - containerCentre) / 2.0}px"
            )
        }
    }
}
