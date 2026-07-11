package dev.slne.surf.api.core.collection

import dev.slne.surf.api.core.api.collection.TransformingObjectSet
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransformingObjectSetTest {
    @Test
    fun `view filters untransformable elements consistently`() {
        val source = ObjectOpenHashSet(listOf(1, 2, 3))
        val view = TransformingObjectSet(
            source,
            { value -> value.takeIf { it % 2 == 0 }?.toString() },
            { value -> value.toIntOrNull()?.takeIf { it % 2 == 0 } }
        )

        assertEquals(listOf("2"), view.toList())
        assertEquals(1, view.size)
        assertFalse(view.isEmpty())
        assertTrue(view.contains("2"))
        assertFalse(view.contains("invalid"))
        assertFalse(view.containsAll(listOf("2", "invalid")))
    }

    @Test
    fun `invalid reverse transformations do not insert null`() {
        val source = ObjectOpenHashSet<Int>()
        val view = TransformingObjectSet(source, Int::toString, String::toIntOrNull)

        assertFalse(view.add("invalid"))
        assertTrue(source.isEmpty())
    }
}
