package dev.slne.surf.api.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `all primitive array families convert to specialized sets`() {
        assertEquals(setOf(true, false), booleanArrayOf(true, false, true).toBooleanSet())
        assertEquals(setOf<Byte>(1, 2), byteArrayOf(1, 2, 1).toByteSet())
        assertEquals(setOf('a', 'b'), charArrayOf('a', 'b', 'a').toCharSet())
        assertEquals(setOf<Short>(1, 2), shortArrayOf(1, 2, 1).toShortSet())
        assertEquals(setOf(1L, 2L), longArrayOf(1, 2, 1).toLongSet())
        assertEquals(setOf(1f, 2f), floatArrayOf(1f, 2f, 1f).toFloatSet())
        assertEquals(setOf(1.0, 2.0), doubleArrayOf(1.0, 2.0, 1.0).toDoubleSet())
    }

    @Test
    fun `object factories filter null values and preserve order for lists`() {
        assertEquals(listOf("first", "second"), objectListOfNotNull("first", null, "second"))
        assertEquals(setOf("first", "second"), objectSetOfNotNull("first", null, "second"))
    }

    @Test
    fun `frozen collections reject mutation`() {
        val list = mutableObjectListOf("value").freeze()
        val set = mutableIntSetOf(1).freeze()
        val map = mutableObject2ObjectMapOf("key" to "value").freeze()

        assertFailsWith<UnsupportedOperationException> { list.add("other") }
        assertFailsWith<UnsupportedOperationException> { set.add(2) }
        assertFailsWith<UnsupportedOperationException> { map["other"] = "value" }
    }

    @Test
    fun `object multimap removes keys when their final value is removed`() {
        val map = mutableObject2MultiObjectsMapOf<String, Int>()
        map.add("key", 1)
        map.addAll("key", listOf(2, 3))

        map.removeAll("key", listOf(1, 2))
        assertTrue("key" in map)
        assertEquals(setOf(3), map["key"]?.toSet())

        map.remove("key", 3)
        assertFalse("key" in map)
    }

    @Test
    fun `primitive and object maps convert to pair lists`() {
        val primitive = mutableInt2LongMapOf(1 to 2L, 3 to 4L)
        val objects = mutableObject2IntMapOf("one" to 1, "two" to 2)

        assertEquals(setOf(1 to 2L, 3 to 4L), primitive.toObjectList().toSet())
        assertEquals(setOf("one" to 1, "two" to 2), objects.toObjectList().toSet())
    }

    @Test
    fun `sequence conversions handle empty singleton and duplicate inputs`() {
        assertTrue(emptySequence<Int>().toIntSet().isEmpty())
        assertEquals(setOf(4), sequenceOf(4).toIntSet())
        assertEquals(setOf(1, 2), sequenceOf(1, 2, 1).toIntSet())
    }
}
