package dev.slne.surf.database.config

import org.slf4j.event.Level

internal object DatabaseEnvironmentVariables {
    const val LOG_LEVEL = "SURF_DATABASE_LOG_LEVEL"
    const val TYPE = "SURF_DATABASE_TYPE"
    const val SCHEMA = "SURF_DATABASE_SCHEMA"
    const val HOST = "SURF_DATABASE_HOST"
    const val PORT = "SURF_DATABASE_PORT"
    const val NAME = "SURF_DATABASE_NAME"
    const val USERNAME = "SURF_DATABASE_USERNAME"
    const val PASSWORD = "SURF_DATABASE_PASSWORD"
    const val POOL_INITIAL_SIZE = "SURF_DATABASE_POOL_INITIAL_SIZE"
    const val POOL_MIN_IDLE = "SURF_DATABASE_POOL_MIN_IDLE"
    const val POOL_MAX_SIZE = "SURF_DATABASE_POOL_MAX_SIZE"
    const val POOL_MAX_ACQUIRE_TIME_MILLIS = "SURF_DATABASE_POOL_MAX_ACQUIRE_TIME_MILLIS"
    const val POOL_MAX_CREATE_CONNECTION_TIME_MILLIS =
        "SURF_DATABASE_POOL_MAX_CREATE_CONNECTION_TIME_MILLIS"
    const val POOL_MAX_VALIDATION_TIME_MILLIS = "SURF_DATABASE_POOL_MAX_VALIDATION_TIME_MILLIS"
    const val POOL_MAX_IDLE_TIME_MILLIS = "SURF_DATABASE_POOL_MAX_IDLE_TIME_MILLIS"
    const val POOL_MAX_LIFE_TIME_MILLIS = "SURF_DATABASE_POOL_MAX_LIFE_TIME_MILLIS"
}

internal fun interface DatabaseEnvironmentVariableLookup {
    operator fun get(name: String): String?
}

internal fun DatabaseConfig.withEnvironmentOverrides(
    environment: DatabaseEnvironmentVariableLookup = DatabaseEnvironmentVariableLookup(System::getenv),
): DatabaseConfig {
    val overridden = copy(
        logLevel = environment.enum(DatabaseEnvironmentVariables.LOG_LEVEL, logLevel),
        credentials = credentials.copy(
            databaseType = environment.enum(DatabaseEnvironmentVariables.TYPE, credentials.databaseType),
            schema = environment.text(DatabaseEnvironmentVariables.SCHEMA, credentials.schema),
            host = environment.text(DatabaseEnvironmentVariables.HOST, credentials.host),
            port = environment.integer(DatabaseEnvironmentVariables.PORT, credentials.port),
            database = environment.text(DatabaseEnvironmentVariables.NAME, credentials.database),
            username = environment.text(DatabaseEnvironmentVariables.USERNAME, credentials.username),
            password = environment.text(
                DatabaseEnvironmentVariables.PASSWORD,
                credentials.password,
                allowBlank = true,
            ),
        ),
        pool = pool.copy(
            sizing = pool.sizing.copy(
                initialSize = environment.integer(
                    DatabaseEnvironmentVariables.POOL_INITIAL_SIZE,
                    pool.sizing.initialSize,
                ),
                minIdle = environment.integer(
                    DatabaseEnvironmentVariables.POOL_MIN_IDLE,
                    pool.sizing.minIdle,
                ),
                maxSize = environment.integer(
                    DatabaseEnvironmentVariables.POOL_MAX_SIZE,
                    pool.sizing.maxSize,
                ),
            ),
            timeouts = pool.timeouts.copy(
                maxAcquireTimeMillis = environment.long(
                    DatabaseEnvironmentVariables.POOL_MAX_ACQUIRE_TIME_MILLIS,
                    pool.timeouts.maxAcquireTimeMillis,
                ),
                maxCreateConnectionTimeMillis = environment.long(
                    DatabaseEnvironmentVariables.POOL_MAX_CREATE_CONNECTION_TIME_MILLIS,
                    pool.timeouts.maxCreateConnectionTimeMillis,
                ),
                maxValidationTimeMillis = environment.long(
                    DatabaseEnvironmentVariables.POOL_MAX_VALIDATION_TIME_MILLIS,
                    pool.timeouts.maxValidationTimeMillis,
                ),
                maxIdleTimeMillis = environment.long(
                    DatabaseEnvironmentVariables.POOL_MAX_IDLE_TIME_MILLIS,
                    pool.timeouts.maxIdleTimeMillis,
                ),
                maxLifeTimeMillis = environment.long(
                    DatabaseEnvironmentVariables.POOL_MAX_LIFE_TIME_MILLIS,
                    pool.timeouts.maxLifeTimeMillis,
                ),
            ),
        ),
    )

    overridden.validate()
    return overridden
}

private fun DatabaseConfig.validate() {
    require(credentials.port in 1..65535) {
        "Database port must be in the range 1..65535. Configure ${DatabaseEnvironmentVariables.PORT}."
    }

    val sizing = pool.sizing
    require(sizing.initialSize >= 0) {
        "Database pool initialSize must not be negative. Configure ${DatabaseEnvironmentVariables.POOL_INITIAL_SIZE}."
    }
    require(sizing.minIdle >= 0) {
        "Database pool minIdle must not be negative. Configure ${DatabaseEnvironmentVariables.POOL_MIN_IDLE}."
    }
    require(sizing.maxSize > 0) {
        "Database pool maxSize must be positive. Configure ${DatabaseEnvironmentVariables.POOL_MAX_SIZE}."
    }
    require(sizing.initialSize <= sizing.maxSize) {
        "Database pool initialSize must not exceed maxSize. Configure " +
                "${DatabaseEnvironmentVariables.POOL_INITIAL_SIZE} and ${DatabaseEnvironmentVariables.POOL_MAX_SIZE}."
    }
    require(sizing.minIdle <= sizing.maxSize) {
        "Database pool minIdle must not exceed maxSize. Configure " +
                "${DatabaseEnvironmentVariables.POOL_MIN_IDLE} and ${DatabaseEnvironmentVariables.POOL_MAX_SIZE}."
    }
}

private inline fun <reified T : Enum<T>> DatabaseEnvironmentVariableLookup.enum(
    name: String,
    fallback: T,
): T {
    val rawValue = this[name] ?: return fallback
    return enumValues<T>().firstOrNull { it.name.equals(rawValue, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "Environment variable $name must be one of: " +
                    enumValues<T>().joinToString { it.name.lowercase() } + "."
        )
}

private fun DatabaseEnvironmentVariableLookup.text(
    name: String,
    fallback: String,
    allowBlank: Boolean = false,
): String {
    val value = this[name] ?: return fallback
    require(allowBlank || value.isNotBlank()) {
        "Environment variable $name must not be blank."
    }
    return value
}

private fun DatabaseEnvironmentVariableLookup.integer(name: String, fallback: Int): Int {
    val rawValue = this[name] ?: return fallback
    return rawValue.toIntOrNull()
        ?: throw IllegalArgumentException("Environment variable $name must be an integer.")
}

private fun DatabaseEnvironmentVariableLookup.long(name: String, fallback: Long): Long {
    val rawValue = this[name] ?: return fallback
    return rawValue.toLongOrNull()
        ?: throw IllegalArgumentException("Environment variable $name must be a long integer.")
}
