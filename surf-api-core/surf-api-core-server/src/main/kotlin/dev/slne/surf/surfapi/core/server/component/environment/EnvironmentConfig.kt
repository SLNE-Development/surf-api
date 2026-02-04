package dev.slne.surf.surfapi.core.server.component.environment

import java.io.File
import java.util.Properties

/**
 * Global environment configuration for the surf-api component system.
 *
 * This singleton manages the global environment setting that is read from a
 * `surf-api.env` file. The environment can be used to conditionally load
 * components based on the current runtime environment (development, production, etc.).
 *
 * The environment file is searched for in the following locations:
 * 1. The directory specified by the "surf.api.config.dir" system property
 * 2. The current working directory
 * 3. The user's home directory under `.surf-api/`
 *
 * Example surf-api.env file content:
 * ```
 * SURF_ENVIRONMENT=development
 * ```
 */
object EnvironmentConfig {
    private const val ENV_FILE_NAME = "surf-api.env"
    private const val ENV_KEY = "SURF_ENVIRONMENT"
    private const val DEFAULT_ENVIRONMENT = "production"

    @Volatile
    private var cachedEnvironment: String? = null

    /**
     * Returns the current environment name.
     *
     * The environment is determined in the following order of precedence:
     * 1. System property `surf.api.environment`
     * 2. Environment variable `SURF_ENVIRONMENT`
     * 3. Value from `surf-api.env` file
     * 4. Default value ("production")
     */
    fun getEnvironment(): String {
        cachedEnvironment?.let { return it }

        synchronized(this) {
            cachedEnvironment?.let { return it }

            val environment = resolveEnvironment()
            cachedEnvironment = environment
            return environment
        }
    }

    /**
     * Checks if the current environment matches any of the specified environments.
     *
     * @param environments The list of environment names to check against
     * @return true if the current environment matches any of the specified environments
     */
    fun matchesAny(environments: Collection<String>): Boolean {
        val current = getEnvironment()
        return environments.any { it.equals(current, ignoreCase = true) }
    }

    /**
     * Resets the cached environment value, forcing a re-read on next access.
     * This is primarily useful for testing purposes.
     */
    fun reset() {
        synchronized(this) {
            cachedEnvironment = null
        }
    }

    private fun resolveEnvironment(): String {
        // 1. Check system property
        System.getProperty("surf.api.environment")?.takeIf { it.isNotBlank() }?.let {
            return it.trim().lowercase()
        }

        // 2. Check environment variable
        System.getenv(ENV_KEY)?.takeIf { it.isNotBlank() }?.let {
            return it.trim().lowercase()
        }

        // 3. Check surf-api.env file in various locations
        findEnvFile()?.let { file ->
            try {
                val properties = Properties()
                file.inputStream().use { properties.load(it) }
                properties.getProperty(ENV_KEY)?.takeIf { it.isNotBlank() }?.let {
                    return it.trim().lowercase()
                }
            } catch (_: Exception) {
                // Ignore file read errors, fall through to default
            }
        }

        // 4. Return default
        return DEFAULT_ENVIRONMENT
    }

    private fun findEnvFile(): File? {
        // Check custom config directory
        System.getProperty("surf.api.config.dir")?.let { dir ->
            val file = File(dir, ENV_FILE_NAME)
            if (file.exists() && file.isFile) return file
        }

        // Check current working directory
        val cwd = File(ENV_FILE_NAME)
        if (cwd.exists() && cwd.isFile) return cwd

        // Check user home directory
        val home = File(System.getProperty("user.home"), ".surf-api/$ENV_FILE_NAME")
        if (home.exists() && home.isFile) return home

        return null
    }
}
