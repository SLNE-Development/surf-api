package dev.slne.surf.api.core.server.component.environment

import dev.slne.surf.api.core.server.environment.EnvironmentAccessor
import dev.slne.surf.api.shared.api.component.requirement.ConditionalOnEnvironment

object EnvironmentConfig {

    @Volatile
    private var cachedEnvironments: List<String>? = null

    fun getEnvironments(): List<String> {
        cachedEnvironments?.let { return it }
        synchronized(this) {
            cachedEnvironments?.let { return it }

            val env = EnvironmentAccessor.getEnv(ConditionalOnEnvironment.ENV_PROPERTY_NAME)
            val resolvedEnvironments = if (env == null) {
                listOf(ConditionalOnEnvironment.PRODUCTION_ENVIRONMENT)
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