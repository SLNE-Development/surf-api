package dev.slne.surf.surfapi.shared.api.component.requirement

/**
 * Specifies that the component depends on at least one of the specified plugins being loaded.
 *
 * The component will only be loaded if at least one of the specified plugins is present.
 * This is useful when a component can work with multiple alternative plugins.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple "one of" plugin groups to be specified.
 *
 * Example:
 * ```kotlin
 * // Load if either Vault or another economy plugin is present
 * @DependsOnOnePlugin(["Vault", "EssentialsX", "CMI"])
 * @ComponentMeta
 * class EconomyIntegration : AbstractComponent() { ... }
 * ```
 *
 * @property pluginIds Array of plugin IDs where at least one must be loaded
 *
 * @see DependsOnPlugin
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class DependsOnOnePlugin(val pluginIds: Array<String>)