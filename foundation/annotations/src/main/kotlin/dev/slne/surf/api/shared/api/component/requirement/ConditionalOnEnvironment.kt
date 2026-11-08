package dev.slne.surf.api.shared.api.component.requirement

/**
 * Specifies that the component should only be loaded in certain environments.
 *
 * The component will only be instantiated if the current environment matches
 * one of the specified environment names. This is useful for components that
 * should only run in development, production, or test environments.
 *
 * This annotation can be used on component classes directly or on meta-annotations.
 * It is repeatable, allowing multiple environment constraints to be specified.
 *
 * Example:
 * ```kotlin
 * @ConditionalOnEnvironment(environments = ["development", "test"])
 * @ComponentMeta
 * class DebugComponent : AbstractComponent() { ... }
 * ```
 *
 * @property environments Array of environment names in which the component should be loaded
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ConditionalOnEnvironment(val environments: Array<String>) {
    companion object {
        /**
         * Constant representing the production environment.
         *
         * This value is used to indicate that certain components or features
         * should only be loaded or executed in the production environment.
         *
         * It can be utilized in conjunction with environment-specific annotations,
         * such as `@ConditionalOnEnvironment`, to define the behavior or configuration
         * of components based on the current runtime environment.
         */
        const val PRODUCTION_ENVIRONMENT = "production"

        /**
         * Specifies the environment name used to identify a development environment.
         *
         * This constant is typically used in configuration or conditional annotations
         * to denote that certain components or features should only be active during
         * the development phase of the application's lifecycle.
         *
         * It is part of the predefined environment constants, alongside production
         * and other potential environment indicators, and serves as a standard marker
         * to control environment-specific behaviors.
         */
        const val DEVELOPMENT_ENVIRONMENT = "development"

        /**
         * The name of the environment property used to determine the application environment.
         *
         * This constant holds the key for retrieving the environment configuration from
         * the system properties, environment variables, or other configuration sources.
         * It is typically used to specify which operational environment the application
         * is running in, such as "development", "testing", or "production".
         *
         * Expected values for this property should align with the predefined environments
         * such as "development" or "production" for consistency across components.
         *
         * If you need to configure multiple environments, you put a comma-separated list
         * of environment names in the property value.
         * For example, "SURF_ENVIRONMENT=development,test".
         */
        const val ENV_PROPERTY_NAME = "SURF_ENVIRONMENT"
    }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnEnvironment(environments = [ConditionalOnEnvironment.PRODUCTION_ENVIRONMENT])
annotation class ConditionalOnProductionEnvironment

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnEnvironment(environments = [ConditionalOnEnvironment.DEVELOPMENT_ENVIRONMENT])
annotation class ConditionalOnDevelopmentEnvironment