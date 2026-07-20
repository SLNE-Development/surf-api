package dev.slne.surf.microservice.gradle.plugin

internal data class EnvironmentVariableDescriptor(
    val name: String,
    val description: String,
    val secret: Boolean = false,
)

internal object DockerEnvironmentContracts {
    const val RABBITMQ_GROUP = "dev.slne.surf.rabbitmq"
    const val DATABASE_GROUP = "dev.slne.surf"
    const val DATABASE_MODULE = "surf-database-r2dbc"

    val rabbitMQ = listOf(
        EnvironmentVariableDescriptor("SURF_RABBITMQ_HOST", "RabbitMQ service host"),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_PORT", "RabbitMQ service port"),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_USERNAME", "RabbitMQ username", secret = true),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_PASSWORD", "RabbitMQ password", secret = true),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_VHOST", "RabbitMQ virtual host"),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_TIMEOUT", "connection timeout in seconds"),
        EnvironmentVariableDescriptor(
            "SURF_RABBITMQ_REQUEST_TIMEOUT_SECONDS",
            "request timeout in seconds",
        ),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_PUBLISHER_POOL_SIZE", "publisher worker count"),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_SERVER_PREFETCH_COUNT", "server prefetch count"),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_PERSIST_REQUESTS", "whether requests are persistent"),
        EnvironmentVariableDescriptor("SURF_RABBITMQ_PERSIST_RESPONSES", "whether responses are persistent"),
        EnvironmentVariableDescriptor(
            "SURF_RABBITMQ_OUTGOING_REQUEST_CHUNKING_ENABLED",
            "whether outgoing requests may be chunked",
        ),
        EnvironmentVariableDescriptor(
            "SURF_RABBITMQ_OUTGOING_RESPONSE_CHUNKING_ENABLED",
            "whether outgoing responses may be chunked",
        ),
    )

    val database = listOf(
        EnvironmentVariableDescriptor("SURF_DATABASE_LOG_LEVEL", "database SQL log level"),
        EnvironmentVariableDescriptor("SURF_DATABASE_TYPE", "mariadb or postgresql"),
        EnvironmentVariableDescriptor("SURF_DATABASE_SCHEMA", "PostgreSQL schema"),
        EnvironmentVariableDescriptor("SURF_DATABASE_HOST", "database service host"),
        EnvironmentVariableDescriptor("SURF_DATABASE_PORT", "database service port"),
        EnvironmentVariableDescriptor("SURF_DATABASE_NAME", "database name"),
        EnvironmentVariableDescriptor("SURF_DATABASE_USERNAME", "database username", secret = true),
        EnvironmentVariableDescriptor("SURF_DATABASE_PASSWORD", "database password", secret = true),
        EnvironmentVariableDescriptor("SURF_DATABASE_POOL_INITIAL_SIZE", "initial connection count"),
        EnvironmentVariableDescriptor("SURF_DATABASE_POOL_MIN_IDLE", "minimum idle connection count"),
        EnvironmentVariableDescriptor("SURF_DATABASE_POOL_MAX_SIZE", "maximum connection count"),
        EnvironmentVariableDescriptor(
            "SURF_DATABASE_POOL_MAX_ACQUIRE_TIME_MILLIS",
            "maximum acquisition time in milliseconds",
        ),
        EnvironmentVariableDescriptor(
            "SURF_DATABASE_POOL_MAX_CREATE_CONNECTION_TIME_MILLIS",
            "maximum connection creation time in milliseconds",
        ),
        EnvironmentVariableDescriptor(
            "SURF_DATABASE_POOL_MAX_VALIDATION_TIME_MILLIS",
            "maximum validation time in milliseconds",
        ),
        EnvironmentVariableDescriptor(
            "SURF_DATABASE_POOL_MAX_IDLE_TIME_MILLIS",
            "maximum idle time in milliseconds",
        ),
        EnvironmentVariableDescriptor(
            "SURF_DATABASE_POOL_MAX_LIFE_TIME_MILLIS",
            "maximum connection lifetime in milliseconds",
        ),
    )
}
