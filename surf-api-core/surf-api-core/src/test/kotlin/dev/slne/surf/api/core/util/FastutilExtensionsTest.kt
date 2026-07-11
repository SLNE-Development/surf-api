package dev.slne.surf.api.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FastutilExtensionsTest {
    @Test
    fun `primitive arrays convert directly to specialized sets`() {
        val values = intArrayOf(1, 2, 2)

        assertEquals(setOf(1, 2), values.toIntSet())
        assertEquals(setOf(1, 2), values.toMutableIntSet())
    }

    @Test
    fun `double arrays have symmetric specialized list conversions`() {
        val values = doubleArrayOf(1.0, 2.0)

        assertEquals(listOf(1.0, 2.0), values.toDoubleList())
        assertEquals(listOf(1.0, 2.0), values.toMutableDoubleList())
    }
}
