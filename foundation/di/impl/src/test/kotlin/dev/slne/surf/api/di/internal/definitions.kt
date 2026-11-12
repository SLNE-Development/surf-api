package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.Scope
import dev.slne.surf.api.di.definition.BeanConditions
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.CollectionKind
import dev.slne.surf.api.di.definition.FactoryDefinition
import dev.slne.surf.api.di.definition.FactoryKind
import dev.slne.surf.api.di.definition.InjectionPoint

/**
 * Builds a bean definition for a test.
 *
 * The tests work on metadata rather than on real classes, which is the point of keeping the whole
 * decision in metadata: a graph or a resolution case can be written down directly, including the
 * ones that are awkward to express as actual Kotlin classes.
 *
 * [provides] defaults to the class name alone, so a test only states the extra types it cares about.
 */
internal fun definition(
    className: String,
    provides: List<String> = listOf(className),
    needs: List<InjectionPoint> = emptyList(),
    scope: Scope = Scope.SINGLETON,
    qualifier: String? = null,
    primary: Boolean = false,
    priority: Int = 0,
    lifecycle: Boolean = false,
    postProcessor: Boolean = false,
    factories: List<FactoryDefinition> = emptyList(),
    dependsOn: List<String> = emptyList(),
) = BeanDefinition(
    className = className,
    provides = provides,
    scope = scope,
    qualifier = qualifier,
    primary = primary,
    priority = priority,
    lifecycle = lifecycle,
    postProcessor = postProcessor,
    ctorParams = needs,
    factories = factories,
    conditions = BeanConditions(dependsOnBeans = dependsOn),
)

/** An injection point taking exactly one bean of [type]. */
internal fun needs(type: String, qualifier: String? = null) =
    InjectionPoint(type = type, qualifier = qualifier)

/** An injection point that tolerates the bean being absent. */
internal fun needsOptional(type: String) = InjectionPoint(type = type, optional = true)

/** An injection point resolved on first access, which creates no edge in the graph. */
internal fun needsLazy(type: String) = InjectionPoint(type = type, lazy = true)

/** An injection point taking every bean of [type]. */
internal fun needsAll(type: String, kind: CollectionKind = CollectionKind.LIST) =
    InjectionPoint(type = type, collection = kind)

/** A `@Bean` function publishing [returnType]. */
internal fun factory(
    memberName: String,
    returnType: String,
    provides: List<String> = listOf(returnType),
    params: List<InjectionPoint> = emptyList(),
    scope: Scope = Scope.SINGLETON,
    qualifier: String? = null,
    primary: Boolean = false,
) = FactoryDefinition(
    memberName = memberName,
    kind = FactoryKind.FUNCTION,
    returnType = returnType,
    provides = provides,
    scope = scope,
    qualifier = qualifier,
    primary = primary,
    params = params,
)

/** Builds an index straight from definitions. */
internal fun indexOf(vararg definitions: BeanDefinition) = BeanIndex(definitions.toList())

/** The ids of the providers an ordering produced, which is what the graph tests assert on. */
internal fun List<BeanProvider>.ids() = map { it.id }
