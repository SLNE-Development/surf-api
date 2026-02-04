package dev.slne.surf.surfapi.core.server.component

import com.google.common.collect.Maps
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.shared.internal.hook.PluginComponentMeta
import java.util.*


object ComponentSorting {
    /**
     * Performs a topological sort on the components, respecting dependencies.
     *
     * This method considers:
     * - Component dependencies (from @DependsOnComponent)
     * - Missing component conditions (from @ConditionalOnMissingComponent) - these create
     *   implicit dependencies where a component with this condition should be loaded after
     *   the component it checks for, so we can properly evaluate if it's missing.
     *
     * @param componentsMeta List of component metadata to sort
     * @return Sorted list of components in dependency order
     * @throws IllegalStateException if a cyclic dependency is detected
     */
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

            // Add edges for explicit component dependencies
            for (dependency in candidate.componentDependencies) {
                val depCandidate = candidateMap[dependency]
                if (depCandidate != null) {
                    graph.putEdge(candidate, depCandidate)
                }
            }

            // Add edges for missing component conditions
            // If a component has @ConditionalOnMissingComponent(X::class), it should be
            // processed after X (if X exists), so we can check if X was actually loaded
            for (missingComponent in candidate.conditionalOnMissingComponents) {
                val missingCandidate = candidateMap[missingComponent]
                if (missingCandidate != null) {
                    // The component with the condition depends on the referenced component
                    // being processed first (so we know if it's loaded or not)
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