package dev.slne.surf.surfapi.core.server.component.environment

import dev.slne.surf.surfapi.core.server.environment.EnvironmentAccessor
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnEnvironment
import it.unimi.dsi.fastutil.objects.ObjectList

object EnvironmentConfig {

    @Volatile
    private var cachedEnvironments: ObjectList<String>? = null

    fun getEnvironments(): ObjectList<String> {
        cachedEnvironments?.let { return it }
        synchronized(this) {
            cachedEnvironments?.let { return it }

            val env = EnvironmentAccessor.getEnv(ConditionalOnEnvironment.ENV_PROPERTY_NAME)
            val resolvedEnvironments = if (env == null) {
                ObjectList.of(ConditionalOnEnvironment.PRODUCTION_ENVIRONMENT)
            } else {
                env.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toTypedArray()
                    .let { ObjectList.of(*it) }
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