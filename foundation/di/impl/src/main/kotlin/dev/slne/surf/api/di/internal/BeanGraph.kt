package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.definition.InjectionPoint

/**
 * Orders providers so that nothing is created before what it needs.
 *
 * The edges come from the injection points themselves, so the order follows what the code actually
 * declares rather than a hand-maintained list. Two kinds of parameter deliberately create no edge:
 *
 * - a `Lazy<T>` parameter, because resolving it is postponed to first access — this is the one way
 *   to break a cycle on purpose
 * - a parameter whose type nothing provides and which is optional, because it will be `null`
 *
 * A collection parameter creates an edge to every provider of its element type: all of them have to
 * exist before the collection can be handed over.
 *
 * `@Priority` only separates providers the graph leaves unordered. It can never move a provider
 * before something it depends on.
 */
internal object BeanGraph {

    /**
     * Returns [index]'s providers in creation order.
     *
     * @param parentProvides types a parent container already provides, which therefore need no edge
     * @throws CyclicDependencyException if the providers depend on each other in a circle
     */
    fun order(index: BeanIndex, parentProvides: (String) -> Boolean = { false }): List<BeanProvider> {
        val byPriority = index.providers.sortedWith(compareBy({ it.priority }, { it.id }))
        val edges = byPriority.associateWith { provider -> dependenciesOf(provider, index, parentProvides) }

        val ordered = ArrayList<BeanProvider>(byPriority.size)
        val marks = HashMap<String, Mark>(byPriority.size)
        val path = ArrayDeque<BeanProvider>()

        byPriority.forEach { visit(it, edges, marks, ordered, path) }

        return ordered
    }

    /** The providers [provider] has to be created after. */
    private fun dependenciesOf(
        provider: BeanProvider,
        index: BeanIndex,
        parentProvides: (String) -> Boolean,
    ): List<BeanProvider> {
        val required = provider.dependencies.filterNot { it.lazy }
        val ordering = provider.definition.conditions.dependsOnBeans.map { InjectionPoint(type = it) }

        return (required + ordering)
            .flatMap { point ->
                when {
                    point.collection != null -> index.resolveAll(point)
                    parentProvides(point.type) && index.providersOf(point.type).isEmpty() -> emptyList()
                    else -> listOfNotNull(index.resolveSingle(point))
                }
            }
            .filter { it.id != provider.id }
            .distinctBy { it.id }
    }

    private fun visit(
        provider: BeanProvider,
        edges: Map<BeanProvider, List<BeanProvider>>,
        marks: MutableMap<String, Mark>,
        ordered: MutableList<BeanProvider>,
        path: ArrayDeque<BeanProvider>,
    ) {
        when (marks[provider.id]) {
            Mark.DONE -> return
            Mark.VISITING -> {
                path.addLast(provider)
                throw CyclicDependencyException(path.toList())
            }

            null -> Unit
        }

        marks[provider.id] = Mark.VISITING
        path.addLast(provider)

        edges[provider].orEmpty().forEach { visit(it, edges, marks, ordered, path) }

        path.removeLast()
        marks[provider.id] = Mark.DONE
        ordered += provider
    }

    private enum class Mark { VISITING, DONE }
}

/**
 * Raised when providers depend on each other in a circle.
 *
 * The message spells the circle out provider by provider, because the first question on seeing one
 * is always which step to break — and `Lazy<T>` on one of the parameters in the path is the answer.
 */
internal class CyclicDependencyException(cycle: List<BeanProvider>) : IllegalStateException(
    "Cyclic dependency between beans: " +
            cycle.joinToString(" -> ") { it.id } +
            ". Break it by taking one of these as Lazy<T>."
)
