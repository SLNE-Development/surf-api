package dev.slne.surf.microservice.api.health

/**
 * Readiness of a microservice component.
 */
public enum class MicroserviceHealthStatus {
    /**
     * The component is ready.
     */
    HEALTHY,

    /**
     * The component is not ready. Any unhealthy result makes the `health` command fail.
     */
    UNHEALTHY,
}
