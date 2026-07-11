package dev.slne.surf.api.core.random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RandomSelectorTest {
    @Test
    fun `nullable sequence does not terminate when a pick returns null`() {
        val selector = RandomSelector.fromIterable(listOf("value"))

        assertEquals(
            listOf(null, null, null),
            selector.sequenceOrNull(successRate = 0.0).take(3).toList()
        )
    }

    @Test
    fun `nullable sequence continues to produce successful picks`() {
        val selector = RandomSelector.fromIterable(listOf("value"))

        assertEquals(
            listOf("value", "value", "value"),
            selector.sequenceOrNull(successRate = 1.0).take(3).toList()
        )
    }

    @Test
    fun `pick many validates count and samples requested amount`() {
        val selector = RandomSelector.fromIterable(listOf("value"))

        assertEquals(listOf("value", "value"), selector.pickMany(2))
        assertEquals(listOf(null, null), selector.pickManyOrNull(2, successRate = 0.0))
        assertFailsWith<IllegalArgumentException> { selector.pickMany(-1) }
    }
}
