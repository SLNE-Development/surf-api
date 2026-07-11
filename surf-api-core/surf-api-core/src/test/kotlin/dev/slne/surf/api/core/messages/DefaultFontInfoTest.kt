package dev.slne.surf.api.core.messages

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultFontInfoTest {
    @Test
    fun `known and unknown characters resolve predictably`() {
        assertEquals(DefaultFontInfo.A, DefaultFontInfo.getDefaultFontInfo('A'))
        assertEquals(DefaultFontInfo.SPACE, DefaultFontInfo.getDefaultFontInfo(' '))
        assertEquals(DefaultFontInfo.DEFAULT, DefaultFontInfo.getDefaultFontInfo('€'))
    }

    @Test
    fun `bold width leaves spaces unchanged`() {
        assertEquals(DefaultFontInfo.A.length + 1, DefaultFontInfo.A.getBoldLength())
        assertEquals(DefaultFontInfo.SPACE.length, DefaultFontInfo.SPACE.getBoldLength())
    }

    @Test
    fun `pixel width sums character advances`() {
        assertEquals(0, DefaultFontInfo.pixelWidth(""))
        assertEquals(6, DefaultFontInfo.pixelWidth("i "))
    }
}
