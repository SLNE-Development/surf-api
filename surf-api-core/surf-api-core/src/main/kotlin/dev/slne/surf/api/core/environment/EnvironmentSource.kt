package dev.slne.surf.api.core.environment

/**
 * Supplies raw environment-variable values.
 *
 * A source returns `null` only when a variable is missing. Present blank values must be returned as
 * an empty or whitespace-only string so callers can distinguish them from missing variables.
 */
fun interface EnvironmentSource {
    /**
     * Returns the raw value for [name], or `null` when the variable is missing.
     */
    operator fun get(name: String): String?
}
