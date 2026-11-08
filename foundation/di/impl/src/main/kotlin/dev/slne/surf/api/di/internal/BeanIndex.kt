package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.InjectionPoint

/**
 * Answers "which provider gives me this type" without loading a single class.
 *
 * Built once from the definitions a container owns. Resolution over interfaces works because every
 * provider is indexed under its whole `provides` list, not just its own class name — which is also
 * why `provides` has to carry the supertypes.
 *
 * The index is deliberately ignorant of parent containers: it answers for one container's own
 * providers, and walking the hierarchy is the container's job.
 */
internal class BeanIndex(definitions: List<BeanDefinition>) {

    /** Every provider in this container, in no particular order. */
    val providers: List<BeanProvider> = definitions.flatMap { it.providers() }

    private val byType: Map<String, List<BeanProvider>> = buildMap<String, MutableList<BeanProvider>> {
        providers.forEach { provider ->
            provider.provides.forEach { type ->
                getOrPut(type) { mutableListOf() }.add(provider)
            }
        }
    }

    private val byId: Map<String, BeanProvider> = providers.associateBy { it.id }

    /** Providers that can produce [type], or empty. */
    fun providersOf(type: String): List<BeanProvider> = byType[type].orEmpty()

    /** The provider with this [id], or `null`. */
    fun byId(id: String): BeanProvider? = byId[id]

    /**
     * Picks the one provider an injection point should use.
     *
     * Returns `null` when nothing provides the type, which the caller turns into either `null` for
     * an optional parameter or a failure for a required one — the index does not decide which.
     *
     * @throws AmbiguousBeanException if several providers match and neither a qualifier nor
     * [dev.slne.surf.api.di.Primary] picks one. Guessing here would make the choice depend on
     * classpath order, so it fails instead and names the candidates.
     */
    fun resolveSingle(point: InjectionPoint): BeanProvider? {
        val candidates = providersOf(point.type)
            .let { found -> point.qualifier?.let { name -> found.filter { it.qualifier == name } } ?: found }

        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.single()

        val primary = candidates.filter { it.primary }

        return when (primary.size) {
            1 -> primary.single()
            0 -> throw AmbiguousBeanException(point, candidates, hasPrimary = false)
            else -> throw AmbiguousBeanException(point, primary, hasPrimary = true)
        }
    }

    /**
     * Every provider for a collection injection point, ordered by priority and then by id.
     *
     * The id breaks ties so the order is the same on every run, whatever order the metadata files
     * arrived in.
     */
    fun resolveAll(point: InjectionPoint): List<BeanProvider> =
        providersOf(point.type)
            .let { found -> point.qualifier?.let { name -> found.filter { it.qualifier == name } } ?: found }
            .sortedWith(compareBy({ it.priority }, { it.id }))
}

/** Raised when an injection point matches several providers and nothing picks one. */
internal class AmbiguousBeanException(
    point: InjectionPoint,
    candidates: List<BeanProvider>,
    hasPrimary: Boolean,
) : IllegalStateException(
    buildString {
        append(point.type)
        point.qualifier?.let { append(" with qualifier '").append(it).append('\'') }
        if (hasPrimary) {
            append(" is provided by several beans marked @Primary: ")
        } else {
            append(" is provided by several beans and none is marked @Primary: ")
        }
        append(candidates.map { it.id }.sorted().joinToString())
        append(". Mark one with @Primary, or name one with @Qualifier at the injection point.")
    }
)
