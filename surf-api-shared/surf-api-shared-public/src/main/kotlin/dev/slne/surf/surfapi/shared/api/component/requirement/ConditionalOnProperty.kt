package dev.slne.surf.surfapi.shared.api.component.requirement

/**
 * Specifies that the component should only be loaded if a configuration property matches.
 *
 * This annotation allows conditional loading based on configuration values.
 * Properties are looked up from a plugin-specific `properties.yml` file located in the
 * plugin's data folder by default. A custom file path can be specified using the [file]
 * parameter.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple property conditions to be specified.
 *
 * Example:
 * ```kotlin
 * // Uses default properties.yml in plugin folder
 * @ConditionalOnProperty(key = "feature.enabled", havingValue = "true")
 * @ComponentMeta
 * class FeatureComponent : AbstractComponent() { ... }
 *
 * // Load if property is missing or equals "default"
 * @ConditionalOnProperty(key = "mode", havingValue = "default", matchIfMissing = true)
 * @ComponentMeta
 * class DefaultModeComponent : AbstractComponent() { ... }
 *
 * // Use a custom properties file
 * @ConditionalOnProperty(key = "custom.setting", havingValue = "enabled", file = "config/custom.yml")
 * @ComponentMeta
 * class CustomConfigComponent : AbstractComponent() { ... }
 * ```
 *
 * @property key The property key to check. Supports nested keys using dot notation (e.g., "database.host")
 * @property havingValue The expected value. If empty, just checks for property existence.
 * @property matchIfMissing If true, the condition matches when the property is not set.
 *                          Default is false.
 * @property file Optional path to a custom properties file, relative to the plugin's data folder.
 *                If empty, uses the default `properties.yml` file.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnProperty(
    val key: String,
    val havingValue: String = "",
    val matchIfMissing: Boolean = false,
    val file: String = ""
)