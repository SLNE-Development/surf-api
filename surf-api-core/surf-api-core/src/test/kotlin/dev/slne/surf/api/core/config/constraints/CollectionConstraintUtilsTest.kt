package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CollectionConstraintUtilsTest {
    @Test
    fun `size utility supports collections maps object arrays and strings`() {
        assertEquals(2, listOf(1, 2).configSizeOrNull())
        assertEquals(1, mapOf("key" to "value").configSizeOrNull())
        assertEquals(3, arrayOf("a", "b", "c").configSizeOrNull())
        assertEquals(4, "surf".configSizeOrNull())
    }

    @Test
    fun `size utility supports every primitive array family`() {
        assertEquals(1, booleanArrayOf(true).configSizeOrNull())
        assertEquals(2, byteArrayOf(1, 2).configSizeOrNull())
        assertEquals(3, charArrayOf('a', 'b', 'c').configSizeOrNull())
        assertEquals(1, shortArrayOf(1).configSizeOrNull())
        assertEquals(2, intArrayOf(1, 2).configSizeOrNull())
        assertEquals(1, longArrayOf(1).configSizeOrNull())
        assertEquals(2, floatArrayOf(1f, 2f).configSizeOrNull())
        assertEquals(1, doubleArrayOf(1.0).configSizeOrNull())
    }

    @Test
    fun `size utility unwraps string or default values`() {
        assertEquals(5, StringOrDefault.of("value").configSizeOrNull())
        assertNull(StringOrDefault.USE_DEFAULT.configSizeOrNull())
        assertNull(42.configSizeOrNull())
    }
}
