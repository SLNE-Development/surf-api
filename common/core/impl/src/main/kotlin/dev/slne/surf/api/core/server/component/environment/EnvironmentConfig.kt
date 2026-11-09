package dev.slne.surf.api.core.server.component.environment

import dev.slne.surf.api.core.server.environment.EnvironmentAccessor

/**
 * The environments this process runs in.
 *
 * Read once from the environment and cached, because every `@ConditionalOnEnvironment` asks the
 * same question while a container is being built and the answer cannot change inside one run.
 */
object EnvironmentConfig {

    /**
     * The environment variable that names the environments.
     *
     * Several may be named at once as a comma-separated list, for example
     * `SURF_ENVIRONMENT=development,test`.
     */
    const val ENV_PROPERTY_NAME = "SURF_ENVIRONMENT"

    /** What an unset [ENV_PROPERTY_NAME] means. */
    const val PRODUCTION_ENVIRONMENT = "production"

    /** The environment a developer's own server runs in. */
    const val DEVELOPMENT_ENVIRONMENT = "development"

    @Volatile
    private var cachedEnvironments: List<String>? = null

    fun getEnvironments(): List<String> {
        cachedEnvironments?.let { return it }
        synchronized(this) {
            cachedEnvironments?.let { return it }

            val env = EnvironmentAccessor.getEnv(ENV_PROPERTY_NAME)
            val resolvedEnvironments = if (env == null) {
                listOf(PRODUCTION_ENVIRONMENT)
            } else {
                env.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }

            cachedEnvironments = resolvedEnvironments
            return resolvedEnvironments
        }
    }

    fun matchesAny(environments: Collection<String>): Boolean {
        val current = getEnvironments()
        for (env in current) {
            for (search in environments) {
                if (env.equals(search, ignoreCase = true)) {
                    return true
                }
            }
        }

        return false
    }

    fun reset() {
        synchronized(this) {
            cachedEnvironments = null
        }
    }
}
