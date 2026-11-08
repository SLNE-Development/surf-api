package dev.slne.surf.api.core.server.component

import com.google.common.collect.Maps
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.shared.internal.hook.PluginComponentMeta
import java.util.*


@Suppress("UnstableApiUsage")
object ComponentSorting {
    fun topologicalSort(
        componentsMeta: List<PluginComponentMeta.Component>,
    ): List<PluginComponentMeta.Component> {
        val sorted = componentsMeta.sortedWith { first, second ->
            first.priority.compareTo(second.priority)
        }

        val graph = GraphBuilder.directed()
            .allowsSelfLoops(false)
            .expectedNodeCount(sorted.size)
            .build<PluginComponentMeta.Component>()

        val candidateMap = Maps.uniqueIndex(sorted, PluginComponentMeta.Component::className)

        for (candidate in sorted) {
            graph.addNode(candidate)

            for (dependency in candidate.componentDependencies) {
                val depCandidate = candidateMap[dependency]
                if (depCandidate != null) {
                    graph.putEdge(candidate, depCandidate)
                }
            }

            for (missingComponent in candidate.conditionalOnMissingComponents) {
                val missingCandidate = candidateMap[missingComponent]
                if (missingCandidate != null) {
                    graph.putEdge(candidate, missingCandidate)
                }
            }
        }

        val sortedComponents = mutableObjectListOf<PluginComponentMeta.Component>()
        val marks = mutableMapOf<PluginComponentMeta.Component, Mark>()

        for (node in graph.nodes()) {
            visitNode(graph, node, marks, sortedComponents, ArrayDeque())
        }

        return sortedComponents
    }

    private fun visitNode(
        dependencyGraph: Graph<PluginComponentMeta.Component>,
        current: PluginComponentMeta.Component,
        visited: MutableMap<PluginComponentMeta.Component, Mark>,
        sorted: MutableList<PluginComponentMeta.Component>,
        currentDependencyScanStack: Deque<PluginComponentMeta.Component>
    ) {
        val mark = visited.getOrDefault(current, Mark.NOT_VISITED)
        if (mark == Mark.VISITED) {
            return
        }

        if (mark == Mark.VISITING) {
            currentDependencyScanStack.addLast(current)
            val loop = currentDependencyScanStack.joinToString(" -> ") { it.className }
            throw IllegalStateException("Cyclic dependency detected in components: $loop")
        }

        currentDependencyScanStack.addLast(current)
        visited[current] = Mark.VISITING
        for (edge in dependencyGraph.successors(current)) {
            visitNode(dependencyGraph, edge, visited, sorted, currentDependencyScanStack)
        }

        visited[current] = Mark.VISITED
        currentDependencyScanStack.removeLast()
        sorted.add(current)
    }

    private enum class Mark {
        NOT_VISITED,
        VISITING,
        VISITED
    }
}