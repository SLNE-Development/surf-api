package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.FactoryDefinition
import dev.slne.surf.api.di.definition.InjectionPoint

/**
 * Something that can produce one bean.
 *
 * A bean reaches the container two ways: its class is annotated, or a `@Bean` member of another
 * bean returns it. Both end up here so resolution, ordering and scoping are written once. The
 * difference is only [dependencies] — a factory additionally needs the bean that declares it.
 */
internal sealed interface BeanProvider {

    /** Identifies the provider uniquely, and names it in error messages. */
    val id: String

    /** Types this provider's bean can be resolved as. */
    val provides: List<String>

    /** The name from `@Qualifier`, or `null`. */
    val qualifier: String?

    /** Whether this provider wins when several provide the same type. */
    val primary: Boolean

    /** Order among providers the graph leaves unordered, lower first. */
    val priority: Int

    /** How many instances exist and who sees them. */
    val scope: Scope

    /** The definition that owns this provider, for conditions and for loading the class. */
    val definition: BeanDefinition

    /** What has to exist before this provider can run, as injection points. */
    val dependencies: List<InjectionPoint>
}

/** Produces a bean by calling the primary constructor of an annotated class. */
internal data class ClassProvider(override val definition: BeanDefinition) : BeanProvider {
    override val id get() = definition.className
    override val provides get() = definition.provides
    override val qualifier get() = definition.qualifier
    override val primary get() = definition.primary
    override val priority get() = definition.priority
    override val scope
        get() = definition.scope.takeUnless { it == Scope.INHERIT } ?: Scope.SINGLETON

    override val dependencies get() = definition.ctorParams
}

/**
 * Produces a bean by reading a property or calling a function on another bean.
 *
 * [dependencies] lists the factory's own parameters and, first, the declaring bean itself: a factory
 * cannot run before the bean it lives on exists, and recording that as a dependency means the graph
 * enforces it instead of the container special-casing it.
 */
internal data class FactoryProvider(
    override val definition: BeanDefinition,
    val factory: FactoryDefinition,
) : BeanProvider {
    override val id get() = "${definition.className}#${factory.memberName}"
    override val provides get() = factory.provides
    override val qualifier get() = factory.qualifier
    override val primary get() = factory.primary
    override val priority get() = factory.priority
    // A factory's scope may say it takes the declaring bean's, which is resolved here so nothing
    // downstream ever meets Scope.INHERIT. It resolves through the declaring class's provider
    // rather than through the definition, because a definition can itself still say INHERIT and
    // only that provider knows what an unstated scope falls back to.
    override val scope
        get() = factory.scope.takeUnless { it == Scope.INHERIT } ?: ClassProvider(definition).scope
    override val dependencies
        get() = listOf(InjectionPoint(type = definition.className)) + factory.params
}

/** Every provider a definition contributes: its class, plus one per `@Bean` member. */
internal fun BeanDefinition.providers(): List<BeanProvider> =
    listOf(ClassProvider(this)) + factories.map { FactoryProvider(this, it) }
