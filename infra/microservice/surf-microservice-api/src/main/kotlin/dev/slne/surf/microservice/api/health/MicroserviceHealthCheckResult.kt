package dev.slne.surf.microservice.api.health

/**
 * Result of one readiness check.
 *
 * @property component stable component category, such as `database` or `nats`
 * @property instance name of the concrete resource, if the component has several
 * @property message details; required when [status] is [MicroserviceHealthStatus.UNHEALTHY]
 */
data class MicroserviceHealthCheckResult(
    val component: String,
    val instance: String? = null,
    val status: MicroserviceHealthStatus,
    val message: String? = null,
) {
    init {
        require(component.isNotBlank()) { "Health check component must not be blank" }
        require(instance == null || instance.isNotBlank()) { "Health check instance must not be blank" }
        require(message == null || message.isNotBlank()) { "Health check message must not be blank" }
        require(status != MicroserviceHealthStatus.UNHEALTHY || message != null) {
            "An unhealthy health check result must provide a message"
        }
    }
}
