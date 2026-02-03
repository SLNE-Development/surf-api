package dev.slne.surf.surfapi.core.api.algorithms

import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf

private typealias Graph<T> = Map<T, List<T>>

fun <T> Graph<T>.topologicalSortSafe(): Result<List<T>> {
    val graph = this
    val incomingEdges = mutableObject2IntMapOf<T>()
    for ((vertex, successors) in graph) {
        if (vertex !in incomingEdges) {
            incomingEdges[vertex] = 0
        }
        for (successor in successors) {
            incomingEdges.mergeInt(successor, 1, Int::plus)
        }
    }

    val queue = ArrayDeque<T>()
    incomingEdges.object2IntEntrySet().fastForEach {entry ->
        val vertex = entry.key
        val edges = entry.intValue
        if (edges == 0) queue += vertex

    }

    val result = mutableObjectListOf<T>()

    while (queue.isNotEmpty()) {
        val vertex = queue.removeFirst()
        result += vertex

        for (successor in graph[vertex].orEmpty()) {
            incomingEdges.mergeInt(successor, -1, Int::plus)
            if (incomingEdges.getInt(successor) == 0) {
                queue += successor
            }
        }
    }

    if (result.size != incomingEdges.size) {
        return Result.failure(IllegalStateException("Graph contains a cycle, topological sort not possible!"))
    }

    return Result.success(result)
}

fun <T> Graph<T>.topologicalSort(): List<T> {
    return topologicalSortSafe().getOrThrow()
}
