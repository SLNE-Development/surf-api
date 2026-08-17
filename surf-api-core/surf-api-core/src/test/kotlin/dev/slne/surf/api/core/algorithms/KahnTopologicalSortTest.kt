package dev.slne.surf.api.core.algorithms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KahnTopologicalSortTest {

    @Test
    fun `empty graph sorts to empty list`() {
        assertTrue(emptyMap<String, List<String>>().topologicalSort().isEmpty())
    }

    @Test
    fun `linear chain keeps order`() {
        val graph = mapOf("a" to listOf("b"), "b" to listOf("c"))
        assertEquals(listOf("a", "b", "c"), graph.topologicalSort())
    }

    @Test
    fun `diamond respects partial order`() {
        val graph = mapOf(
            "root" to listOf("left", "right"),
            "left" to listOf("sink"),
            "right" to listOf("sink"),
        )
        val order = graph.topologicalSort()
        assertEquals(4, order.size)
        assertPartialOrder(graph, order)
    }

    @Test
    fun `vertex appearing only as successor is included`() {
        val graph = mapOf("a" to listOf("b"))
        assertEquals(listOf("a", "b"), graph.topologicalSort())
    }

    @Test
    fun `cycle returns failure`() {
        val graph = mapOf("a" to listOf("b"), "b" to listOf("a"))
        assertTrue(graph.topologicalSortSafe().isFailure)
    }

    private fun <T> assertPartialOrder(graph: Map<T, List<T>>, order: List<T>) {
        val index = order.withIndex().associate { (i, v) -> v to i }
        graph.forEach { (from, successors) ->
            successors.forEach { to ->
                assertTrue(
                    index.getValue(from) < index.getValue(to),
                    "$from must be ordered before $to",
                )
            }
        }
    }
}
