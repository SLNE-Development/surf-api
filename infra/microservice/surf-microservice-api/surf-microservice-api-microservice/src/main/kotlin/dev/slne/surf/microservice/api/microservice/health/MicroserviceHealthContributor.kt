package dev.slne.surf.microservice.api.microservice.health

/**
 * Contributes component-specific readiness checks to a running microservice.
 *
 * Implementations are discovered automatically through [ServiceLoader][java.util.ServiceLoader]. A provider must therefore
 * have a public no-argument constructor and be declared in the corresponding
 * `META-INF/services` descriptor.
 *
 * A contributor should only report resources that are actually active in the current process.
 * Returning an empty list indicates that the contributor currently has no managed resources and
 * does not make the microservice unhealthy.
 */
interface MicroserviceHealthContributor {

    /**
     * A stable name identifying this contributor.
     *
     * This name is used when the contributor itself fails before it can return individual check
     * results.
     */
    val name: String

    /**
     * Checks all active resources managed by this contributor.
     *
     * Implementations should avoid blocking threads and should return within a short amount of
     * time. The microservice runtime additionally applies a timeout to every contributor.
     *
     * @return one result for every active resource managed by this contributor, or an empty list
     * if no relevant resources are active.
     */
    suspend fun check(): List<MicroserviceHealthCheckResult>
}

/**
 * Describes the binary readiness state of a microservice component.
 */
enum class MicroserviceHealthStatus {
    /**
     * The component is ready to serve requests.
     */
    HEALTHY,

    /**
     * The component is not ready to serve requests.
     */
    UNHEALTHY
}

/**
 * Represents the result of one component readiness check.
 *
 * @property component Stable component category, such as `database` or `rabbitmq`.
 * @property instance Optional name of the concrete resource instance.
 * @property status Readiness status reported for the resource.
 * @property message Optional additional information. An unhealthy result must provide a message.
 */
data class MicroserviceHealthCheckResult(
    val component: String,
    val instance: String? = null,
    val status: MicroserviceHealthStatus,
    val message: String? = null
) {
    init {
        require(component.isNotBlank()) {
            "Health check component must not be blank."
        }

        require(instance == null || instance.isNotBlank()) {
            "Health check instance must not be blank."
        }

        require(message == null || message.isNotBlank()) {
            "Health check message must not be blank."
        }

        require(status != MicroserviceHealthStatus.UNHEALTHY || message != null) {
            "An unhealthy health check result must provide a message."
        }
    }
}