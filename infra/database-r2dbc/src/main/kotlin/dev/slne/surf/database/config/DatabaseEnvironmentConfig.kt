package dev.slne.surf.database.config

import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.api.core.environment.EnvironmentVariableValidation
import dev.slne.surf.api.core.environment.env
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

    val logLevel by env.optional(enumIgnoringCase<Level>()).named(LOG_LEVEL)
    val type by env.optional(enumIgnoringCase<DatabaseType>()).named(TYPE)
    val schema by env.optional(validation = nonBlank).named(SCHEMA)
    val host by env.optional(validation = nonBlank).named(HOST)
    val port by env.optionalInt().named(PORT)
    val name by env.optional(validation = nonBlank).named(NAME)
    val username by env.optional(validation = nonBlank).named(USERNAME)
    val password by env.optional(sensitive = true).named(PASSWORD)
    val poolInitialSize by env.optionalInt().named(POOL_INITIAL_SIZE)
    val poolMinIdle by env.optionalInt().named(POOL_MIN_IDLE)
    val poolMaxSize by env.optionalInt().named(POOL_MAX_SIZE)
    val poolMaxAcquireTimeMillis by env.optionalLong().named(POOL_MAX_ACQUIRE_TIME_MILLIS)
    val poolMaxCreateConnectionTimeMillis by env.optionalLong()
        .named(POOL_MAX_CREATE_CONNECTION_TIME_MILLIS)
    val poolMaxValidationTimeMillis by env.optionalLong().named(POOL_MAX_VALIDATION_TIME_MILLIS)
    val poolMaxIdleTimeMillis by env.optionalLong().named(POOL_MAX_IDLE_TIME_MILLIS)
    val poolMaxLifeTimeMillis by env.optionalLong().named(POOL_MAX_LIFE_TIME_MILLIS)
}

internal fun DatabaseConfig.withEnvironmentOverrides(): DatabaseConfig {
    val environment = DatabaseEnvironmentVariables
    val overridden = copy(
        logLevel = environment.logLevel ?: logLevel,
        credentials = credentials.copy(
            databaseType = environment.type ?: credentials.databaseType,
            schema = environment.schema ?: credentials.schema,
            host = environment.host ?: credentials.host,
            port = environment.port?.let(IntOr::Default) ?: credentials.port,
            database = environment.name ?: credentials.database,
            username = environment.username ?: credentials.username,
            password = environment.password ?: credentials.password,
        ),
        pool = pool.copy(
            sizing = pool.sizing.copy(
                initialSize = environment.poolInitialSize ?: pool.sizing.initialSize,
                minIdle = environment.poolMinIdle ?: pool.sizing.minIdle,
                maxSize = environment.poolMaxSize ?: pool.sizing.maxSize,
            ),
            timeouts = pool.timeouts.copy(
                maxAcquireTimeMillis = environment.poolMaxAcquireTimeMillis
                    ?: pool.timeouts.maxAcquireTimeMillis,
                maxCreateConnectionTimeMillis = environment.poolMaxCreateConnectionTimeMillis
                    ?: pool.timeouts.maxCreateConnectionTimeMillis,
                maxValidationTimeMillis = environment.poolMaxValidationTimeMillis
                    ?: pool.timeouts.maxValidationTimeMillis,
                maxIdleTimeMillis = environment.poolMaxIdleTimeMillis
                    ?: pool.timeouts.maxIdleTimeMillis,
                maxLifeTimeMillis = environment.poolMaxLifeTimeMillis
                    ?: pool.timeouts.maxLifeTimeMillis,
            ),
        ),
    )

    overridden.validate()
    return overridden
}

private fun DatabaseConfig.validate() {
    require(credentials.resolvedPort in 1..65535) {
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

private val nonBlank: EnvironmentVariableValidation<String>.() -> Unit = {
    require("expected a non-blank string") { it.isNotBlank() }
}

private inline fun <reified T : Enum<T>> enumIgnoringCase(): (String) -> T = { rawValue ->
    enumValues<T>().firstOrNull { it.name.equals(rawValue, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "Expected one of: " + enumValues<T>().joinToString { it.name.lowercase() }
        )
}
