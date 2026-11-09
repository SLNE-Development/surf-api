package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.definition.CollectionKind
import dev.slne.surf.api.di.definition.InjectionPoint

/**
 * Resolves beans by type name.
 *
 * The container's public interface works with `KClass`, but everything inside works with the binary
 * names the metadata records, because a name can be matched without loading the class. A container
 * implements this so its parent can be asked the same question.
 */
internal interface BeanLookup {

    /** The container asked next when this one has no candidate, or `null` for the root. */
    val parentLookup: BeanLookup?

    /** The instance providing [type], or `null` if this container and its parents have none. */
    fun resolveByName(type: String, qualifier: String?): Any?

    /** Every instance providing [type] in this container and its parents, nearest container first. */
    fun resolveAllByName(type: String, qualifier: String?): List<Any>

    /** Whether this container or a parent can provide [type] at all. */
    fun provides(type: String): Boolean
}

/**
 * Turns one injection point into the value to pass.
 *
 * The shape of the parameter decides how, which is why [InjectionPoint] records the shape separately
 * from the type:
 *
 * - a collection takes every candidate, including the parents', because a registrar wants all
 *   handlers in the process and not only the ones in its own container
 * - a lazy parameter resolves on first access, so a bean can hold something that does not exist yet
 * - a nullable parameter accepts absence, so an optional integration needs no condition
 * - anything else must resolve to exactly one bean
 */
internal class DependencyResolver(
    private val index: BeanIndex,
    private val lookup: BeanLookup,
    private val instanceOf: (BeanProvider) -> Any,
) {

    /**
     * Returns the value for [point].
     *
     * @param consumer the bean being constructed, named in failure messages — a missing dependency
     * is only actionable if you know who asked for it
     * @throws UnsatisfiedDependencyException if a required parameter has no candidate
     */
    fun resolve(point: InjectionPoint, consumer: String): Any? = when {
        point.collection != null -> resolveCollection(point)
        point.lazy -> lazy { resolveSingle(point.copy(lazy = false), consumer) }
        else -> resolveSingle(point, consumer)
    }

    private fun resolveCollection(point: InjectionPoint): Any {
        val element = point.copy(collection = null)
        val own = index.resolveAll(element).map(instanceOf)
        val inherited = lookup.parentLookup?.resolveAllByName(point.type, point.qualifier).orEmpty()
        val all = own + inherited

        return when (point.collection) {
            CollectionKind.SET -> all.toSet()
            else -> all
        }
    }

    private fun resolveSingle(point: InjectionPoint, consumer: String): Any? {
        index.resolveSingle(point)?.let { return instanceOf(it) }

        lookup.parentLookup?.resolveByName(point.type, point.qualifier)?.let { return it }

        if (point.optional) return null

        throw UnsatisfiedDependencyException(consumer, point)
    }
}

/** Raised when a bean asks for something no container in the hierarchy provides. */
internal class UnsatisfiedDependencyException(
    consumer: String,
    point: InjectionPoint,
) : IllegalStateException(
    buildString {
        append(consumer).append(" needs ").append(point.type)
        point.qualifier?.let { append(" with qualifier '").append(it).append('\'') }
        append(", but no bean provides it. Annotate the implementation, publish it with @Bean, ")
        append("make the parameter nullable if it is genuinely optional, or check whether a ")
        append("condition skipped the bean that would have provided it.")
    }
)
