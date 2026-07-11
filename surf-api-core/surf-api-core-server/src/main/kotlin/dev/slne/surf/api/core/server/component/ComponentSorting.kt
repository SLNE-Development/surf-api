package dev.slne.surf.api.core.server.component

import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.shared.internal.hook.PluginComponentMeta
import java.util.*


object ComponentSorting {
    fun topologicalSort(
        componentsMeta: List<PluginComponentMeta.Component>,
    ): List<PluginComponentMeta.Component> {
        val sorted = componentsMeta.sortedWith { first, second ->
            first.priority.compareTo(second.priority)
        }

        val candidateMap = mutableObject2ObjectMapOf<String, PluginComponentMeta.Component>(sorted.size)
        for (candidate in sorted) {
            require(candidateMap.put(candidate.className, candidate) == null) {
                "Duplicate component class name: ${candidate.className}"
            }
        }

        val sortedComponents = mutableObjectListOf<PluginComponentMeta.Component>()
        val marks = mutableMapOf<PluginComponentMeta.Component, Mark>()

        for (node in sorted) {
            visitNode(node, candidateMap, marks, sortedComponents, ArrayDeque())
        }

        return sortedComponents
    }

    private fun visitNode(
        current: PluginComponentMeta.Component,
        candidates: Map<String, PluginComponentMeta.Component>,
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
        for (dependencyName in current.componentDependencies) {
            val dependency = candidates[dependencyName] ?: continue
            visitNode(dependency, candidates, visited, sorted, currentDependencyScanStack)
        }
        for (dependencyName in current.conditionalOnMissingComponents) {
            val dependency = candidates[dependencyName] ?: continue
            visitNode(dependency, candidates, visited, sorted, currentDependencyScanStack)
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
