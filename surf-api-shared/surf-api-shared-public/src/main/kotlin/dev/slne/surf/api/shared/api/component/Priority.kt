package dev.slne.surf.api.shared.api.component

/**
 * Specifies the priority of a component for initialization ordering.
 *
 * Components with lower priority values are initialized first.
 * If two components have the same priority, their order is determined
 * by their dependencies (via [@DependsOnComponent][dev.slne.surf.api.shared.api.component.requirement.DependsOnComponent]).
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * When used on a meta-annotation, it provides a default priority for all components
 * using that meta-annotation. A direct `@Priority` on the component class overrides
 * the meta-annotation's priority.
 *
 * Example:
 * ```kotlin
 * @ComponentMeta
 * @Priority(10)
 * class LowPriorityComponent : AbstractComponent() { ... }
 *
 * @ComponentMeta
 * @Priority(-10)
 * class HighPriorityComponent : AbstractComponent() { ... }
 * ```
 *
 * Example with meta-annotation:
 * ```kotlin
 * @ComponentMeta
 * @Priority(100)  // Default priority for all @Service components
 * annotation class Service
 *
 * @Service  // Will have priority 100
 * class MyService : AbstractComponent() { ... }
 *
 * @Service
 * @Priority(50)  // Overrides the default
 * class ImportantService : AbstractComponent() { ... }
 * ```
 *
 * @property value The priority value. Lower values are initialized first. Default is 0.
 *
 * @see SurfComponentMeta
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority(
    val value: Short = 0
)