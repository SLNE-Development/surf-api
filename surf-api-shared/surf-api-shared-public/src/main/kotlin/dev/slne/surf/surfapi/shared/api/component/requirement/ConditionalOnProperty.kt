package dev.slne.surf.surfapi.shared.api.component.requirement

/**
 * Specifies that the component should only be loaded if a configuration property matches.
 *
 * This annotation allows conditional loading based on configuration values.
 * The property is looked up in the environment configuration at runtime.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple property conditions to be specified.
 *
 * Example:
 * ```kotlin
 * @ConditionalOnProperty(key = ["feature", "enabled"], havingValue = "true")
 * @ComponentMeta
 * class FeatureComponent : AbstractComponent() { ... }
 *
 * // Load if property is missing or equals "default"
 * @ConditionalOnProperty(key = ["mode"], havingValue = "default", matchIfMissing = true)
 * @ComponentMeta
 * class DefaultModeComponent : AbstractComponent() { ... }
 *
 * // Use a custom properties file
 * @ConditionalOnProperty(key = ["custom", "setting"], havingValue = "enabled", file = "config/custom.yml")
 * @ComponentMeta
 * class CustomConfigComponent : AbstractComponent() { ... }
 * ```
 *
 * @property key The property key to check
 * @property havingValue The expected value. If empty, just checks for property existence.
 * @property matchIfMissing If true, the condition matches when the property is not set.
 *                          Default is false.
 * @property file Optional path to a custom properties file, relative to the plugin's data folder.
 * If empty, uses the default `properties.yml` file.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnProperty(
    val key: Array<String>,
    val havingValue: String = "",
    val matchIfMissing: Boolean = false,
    val file: String = ""
) {

    companion object {
        const val DEFAULT_PROPERTIES_FILE = "properties.yml"
    }
}