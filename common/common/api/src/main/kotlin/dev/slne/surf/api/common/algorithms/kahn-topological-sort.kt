package dev.slne.surf.api.common.algorithms

private typealias Graph<T> = Map<T, List<T>>

fun <T> Graph<T>.topologicalSortSafe(): Result<List<T>> {
    val graph = this
    val incomingEdges = mutableMapOf<T, Int>()
    for ((vertex, successors) in graph) {
        if (vertex !in incomingEdges) {
            incomingEdges[vertex] = 0
        }
        for (successor in successors) {
            incomingEdges.merge(successor, 1, Int::plus)
        }
    }

    val queue = ArrayDeque<T>()
    for ((vertex, edges) in incomingEdges) {
        if (edges == 0) queue += vertex
    }

    val result = mutableListOf<T>()

    while (queue.isNotEmpty()) {
        val vertex = queue.removeFirst()
        result += vertex

        for (successor in graph[vertex].orEmpty()) {
            incomingEdges.merge(successor, -1, Int::plus)
            if (incomingEdges[successor] == 0) {
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
