package dev.slne.surf.microservice.api.health

/**
 * Result of one readiness check.
 *
 * The `health` command lists every unhealthy result as `component: message`, or `component[instance]: message` when
 * [instance] is set.
 *
 * @property component a stable component category, such as `database` or `nats`
 * @property instance the name of the concrete resource if the component has several, otherwise `null`
 * @property status whether the resource is ready
 * @property message details, or `null` for none. Required when [status] is [MicroserviceHealthStatus.UNHEALTHY]
 * @throws IllegalArgumentException if [component], [instance] or [message] is blank, or [status] is
 * [MicroserviceHealthStatus.UNHEALTHY] and [message] is `null`
 */
public data class MicroserviceHealthCheckResult(
    public val component: String,
    public val instance: String? = null,
    public val status: MicroserviceHealthStatus,
    public val message: String? = null,
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
