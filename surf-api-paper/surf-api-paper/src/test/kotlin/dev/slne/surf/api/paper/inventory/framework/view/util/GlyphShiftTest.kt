package dev.slne.surf.api.paper.inventory.framework.view.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GlyphShiftTest {
    @Test
    fun `zero shift renders no glyphs`() {
        assertEquals("", shift(0))
    }

    @Test
    fun `rendered glyph amounts sum to requested positive and negative shifts`() {
        assertEquals(777L, renderedAmount(777))
        assertEquals(-1_025L, renderedAmount(-1_025))
    }

    @Test
    fun `binary decomposition uses a minimal glyph count`() {
        assertEquals(1, shift(512).length)
        assertEquals(2, shift(513).length)
        assertEquals(3, shift(7).length)
    }

    @Test
    fun `glyph lookup accepts only supported signed powers of two`() {
        assertEquals(64, GlyphShift.ShiftGlyph.fromAmount(64)?.amount)
        assertEquals(-256, GlyphShift.ShiftGlyph.fromAmount(-256)?.amount)
        assertNull(GlyphShift.ShiftGlyph.fromAmount(0))
        assertNull(GlyphShift.ShiftGlyph.fromAmount(3))
        assertNull(GlyphShift.ShiftGlyph.fromAmount(1024))
    }

    private fun renderedAmount(amount: Int): Long {
        val byCharacter = GlyphShift.ShiftGlyph.entries.associateBy { it.glyph }
        return shift(amount).sumOf { character ->
            requireNotNull(byCharacter[character]).amount.toLong()
        }
    }
}
