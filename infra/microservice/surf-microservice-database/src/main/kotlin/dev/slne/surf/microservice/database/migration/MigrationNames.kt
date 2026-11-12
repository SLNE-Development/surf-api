package dev.slne.surf.microservice.database.migration

/**
 * Returns the Flyway migration name for a service name, which determines the history table
 * `flyway_schema_history_<name>`.
 */
internal fun migrationName(serviceName: String): String = serviceName.replace('-', '_')
