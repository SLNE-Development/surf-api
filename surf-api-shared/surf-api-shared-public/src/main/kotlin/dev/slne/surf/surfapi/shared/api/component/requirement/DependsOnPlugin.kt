package dev.slne.surf.surfapi.shared.api.component.requirement

/**
 * Specifies that the component depends on a plugin being loaded.
 *
 * The component will only be loaded if the specified plugin is present and loaded.
 * This is useful for creating optional integrations with other plugins.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple plugin dependencies to be specified.
 *
 * Example:
 * ```kotlin
 * // Only load if Vault plugin is present
 * @DependsOnPlugin("Vault")
 * @ComponentMeta
 * class VaultEconomy : AbstractComponent() { ... }
 * ```
 *
 * @property pluginId The plugin ID that must be loaded
 *
 * @see DependsOnOnePlugin
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnPlugin(val pluginId: String)
