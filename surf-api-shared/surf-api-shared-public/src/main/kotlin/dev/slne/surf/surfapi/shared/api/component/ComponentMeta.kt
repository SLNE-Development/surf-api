package dev.slne.surf.surfapi.shared.api.component

/**
 * Annotation to mark a class as a component or to create meta-annotations.
 *
 * When applied to a class, it marks that class as a component that will be
 * automatically discovered and managed by the component system.
 *
 * When applied to another annotation class (meta-annotation), any class annotated
 * with that annotation will also be treated as a component. This enables creating
 * domain-specific component annotations.
 *
 * Example of direct usage:
 * ```kotlin
 * @ComponentMeta
 * @Priority(10)
 * class MyComponent : AbstractComponent() {
 *     override suspend fun onEnable() {
 *         // Component logic
 *     }
 * }
 * ```
 *
 * Example of meta-annotation usage:
 * ```kotlin
 * // Define a custom annotation
 * @ComponentMeta
 * @Priority(100)  // Default priority for all @Service components
 * @Target(AnnotationTarget.CLASS)
 * annotation class Service
 *
 * // Use the custom annotation - class is automatically a component with priority 100
 * @Service
 * class MyService : AbstractComponent() { ... }
 *
 * // Override the default priority
 * @Service
 * @Priority(50)
 * class ImportantService : AbstractComponent() { ... }
 * ```
 *
 * @see Component
 * @see AbstractComponent
 * @see Priority
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ComponentMeta
