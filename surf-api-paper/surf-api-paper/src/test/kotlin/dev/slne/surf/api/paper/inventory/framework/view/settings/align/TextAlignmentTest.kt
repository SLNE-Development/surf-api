package dev.slne.surf.api.paper.inventory.framework.view.settings.align

import kotlin.test.Test
import kotlin.test.assertEquals

class TextAlignmentTest {
    private val options = TextAlignmentOptions(
        leftShift = -8,
        padding = 4,
        containerWidth = 100,
        charSize = 5,
        charSpacing = 1,
    )

    @Test
    fun `text width handles empty and spaced characters`() {
        assertEquals(0, TextAlignment.calculateTextWidth("", options))
        assertEquals(5, TextAlignment.calculateTextWidth("a", options))
        assertEquals(17, TextAlignment.calculateTextWidth("abc", options))
    }

    @Test
    fun `left alignment includes base shift and padding`() {
        assertEquals(-4, TextAlignment.LEFT.calculateShift("abc", options))
    }

    @Test
    fun `center alignment uses half of remaining space`() {
        assertEquals(34, TextAlignment.CENTER.calculateShift("abc", options))
    }

    @Test
    fun `right alignment uses all remaining space`() {
        assertEquals(72, TextAlignment.RIGHT.calculateShift("abc", options))
    }
}
