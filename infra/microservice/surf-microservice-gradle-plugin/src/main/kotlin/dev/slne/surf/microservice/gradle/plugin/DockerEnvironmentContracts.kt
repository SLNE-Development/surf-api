package dev.slne.surf.microservice.gradle.plugin

internal data class DockerEnvironmentVariable(
    val name: String,
    val description: String,
    val secret: Boolean = false,
)

internal enum class DockerEnvironmentContract(
    val displayName: String,
    private val dependencyGroup: String,
    private val dependencyModule: String? = null,
    val variables: List<DockerEnvironmentVariable>,
) {
    RABBITMQ(
        displayName = "RabbitMQ",
        dependencyGroup = "dev.slne.surf.rabbitmq",
        variables = listOf(
            env(
                "SURF_RABBITMQ_HOST",
                "RabbitMQ service host",
            ),
            env(
                "SURF_RABBITMQ_PORT",
                "RabbitMQ service port",
            ),
            env(
                "SURF_RABBITMQ_USERNAME",
                "RabbitMQ username",
                secret = true,
            ),
            env(
                "SURF_RABBITMQ_PASSWORD",
                "RabbitMQ password",
                secret = true,
            ),
            env(
                "SURF_RABBITMQ_VHOST",
                "RabbitMQ virtual host",
            ),
            env(
                "SURF_RABBITMQ_TIMEOUT",
                "connection timeout in seconds",
            ),
            env(
                "SURF_RABBITMQ_REQUEST_TIMEOUT_SECONDS",
                "request timeout in seconds",
            ),
            env(
                "SURF_RABBITMQ_PUBLISHER_POOL_SIZE",
                "publisher worker count",
            ),
            env(
                "SURF_RABBITMQ_SERVER_PREFETCH_COUNT",
                "server prefetch count",
            ),
            env(
                "SURF_RABBITMQ_PERSIST_REQUESTS",
                "whether requests are persistent",
            ),
            env(
                "SURF_RABBITMQ_PERSIST_RESPONSES",
                "whether responses are persistent",
            ),
            env(
                "SURF_RABBITMQ_OUTGOING_REQUEST_CHUNKING_ENABLED",
                "whether outgoing requests may be chunked",
            ),
            env(
                "SURF_RABBITMQ_OUTGOING_RESPONSE_CHUNKING_ENABLED",
                "whether outgoing responses may be chunked",
            ),
        ),
    ),

    DATABASE(
        displayName = "Database",
        dependencyGroup = "dev.slne.surf",
        dependencyModule = "surf-database-r2dbc",
        variables = listOf(
            env(
                "SURF_DATABASE_LOG_LEVEL",
                "database SQL log level",
            ),
            env(
                "SURF_DATABASE_TYPE",
                "mariadb or postgresql",
            ),
            env(
                "SURF_DATABASE_SCHEMA",
                "PostgreSQL schema",
            ),
            env(
                "SURF_DATABASE_HOST",
                "database service host",
            ),
            env(
                "SURF_DATABASE_PORT",
                "database service port",
            ),
            env(
                "SURF_DATABASE_NAME",
                "database name",
            ),
            env(
                "SURF_DATABASE_USERNAME",
                "database username",
                secret = true,
            ),
            env(
                "SURF_DATABASE_PASSWORD",
                "database password",
                secret = true,
            ),
            env(
                "SURF_DATABASE_POOL_INITIAL_SIZE",
                "initial connection count",
            ),
            env(
                "SURF_DATABASE_POOL_MIN_IDLE",
                "minimum idle connection count",
            ),
            env(
                "SURF_DATABASE_POOL_MAX_SIZE",
                "maximum connection count",
            ),
            env(
                "SURF_DATABASE_POOL_MAX_ACQUIRE_TIME_MILLIS",
                "maximum acquisition time in milliseconds",
            ),
            env(
                "SURF_DATABASE_POOL_MAX_CREATE_CONNECTION_TIME_MILLIS",
                "maximum connection creation time in milliseconds",
            ),
            env(
                "SURF_DATABASE_POOL_MAX_VALIDATION_TIME_MILLIS",
                "maximum validation time in milliseconds",
            ),
            env(
                "SURF_DATABASE_POOL_MAX_IDLE_TIME_MILLIS",
                "maximum idle time in milliseconds",
            ),
            env(
                "SURF_DATABASE_POOL_MAX_LIFE_TIME_MILLIS",
                "maximum connection lifetime in milliseconds",
            ),
        ),
    );

    fun isRequiredBy(
        runtimeDependencies: Set<String>,
    ): Boolean = runtimeDependencies.any(::matches)

    fun renderDocumentation(): String = buildString {
        appendLine(
            "# $displayName runtime environment " +
                    "(configure in Coolify; values are not embedded):"
        )

        variables.forEach { variable ->
            val secretSuffix =
                if (variable.secret) {
                    "; configure as a secret"
                } else {
                    ""
                }

            appendLine(
                "# - ${variable.name}: " +
                        "${variable.description}$secretSuffix"
            )
        }
    }.trimEnd()

    private fun matches(coordinate: String): Boolean {
        val separator = coordinate.indexOf(':')

        if (separator < 0) {
            return false
        }

        val group = coordinate.substring(
            startIndex = 0,
            endIndex = separator,
        )
        val module = coordinate.substring(separator + 1)

        return group == dependencyGroup &&
                (dependencyModule == null || module == dependencyModule)
    }
}

private fun env(
    name: String,
    description: String,
    secret: Boolean = false,
): DockerEnvironmentVariable {
    return DockerEnvironmentVariable(
        name = name,
        description = description,
        secret = secret,
    )
}