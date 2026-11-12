package dev.slne.surf.microservice.database.integration

import dev.slne.surf.database.DatabaseApi

/**
 * Holds the [DatabaseApi] while [DatabaseIntegration] is running.
 *
 * Reads are safe from any thread. Only the integration writes, and its lifecycle calls never run concurrently.
 */
internal object DatabaseHolder {
    @Volatile
    private var current: DatabaseApi? = null

    /**
     * @throws IllegalStateException if no database is installed
     */
    val api: DatabaseApi
        get() = checkNotNull(current) {
            "No database is available: the database integration opens it before onBootstrap and closes it after " +
                "onDisable, and runs only if the database is enabled in surfMicroservice { database { } }"
        }

    fun currentOrNull(): DatabaseApi? = current

    fun install(api: DatabaseApi) {
        current = api
    }

    fun uninstall(): DatabaseApi? = current.also { current = null }
}
