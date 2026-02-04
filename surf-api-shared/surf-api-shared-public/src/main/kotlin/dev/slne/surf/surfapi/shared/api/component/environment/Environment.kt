package dev.slne.surf.surfapi.shared.api.component.environment

/**
 * Standard environment types for component conditional loading.
 *
 * These constants can be used with [@ConditionalOnEnvironment][dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnEnvironment]
 * to ensure consistent environment naming across plugins and prevent typos.
 *
 * The environment is determined globally from the `surf-api.env` file located in a global
 * configuration directory. This allows setting the environment once for all plugins.
 *
 * Example usage:
 * ```kotlin
 * @ConditionalOnEnvironment([Environment.DEVELOPMENT, Environment.TEST])
 * @ComponentMeta
 * class DebugOnlyComponent : AbstractComponent() { ... }
 * ```
 *
 * @see dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnEnvironment
 */
object Environment {
    /**
     * Development environment.
     * Use for components that should only run during local development.
     */
    const val DEVELOPMENT = "development"

    /**
     * Production environment.
     * Use for components that should run in production/live servers.
     */
    const val PRODUCTION = "production"

    /**
     * Test environment.
     * Use for components that should only run during testing.
     */
    const val TEST = "test"

    /**
     * Staging environment.
     * Use for components that should run in staging/pre-production servers.
     */
    const val STAGING = "staging"

    /**
     * Local environment.
     * Use for components that should only run in local development setups.
     */
    const val LOCAL = "local"
}
