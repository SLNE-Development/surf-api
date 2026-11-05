package dev.slne.surf.microservice.api.health

/**
 * Contributes readiness checks to the `health` command of a microservice.
 *
 * Implementations are loaded through [ServiceLoader][java.util.ServiceLoader] and need a public no-argument
 * constructor and a `META-INF/services` entry, usually generated with `@AutoService`.
 *
 * A contributor reports only resources that are active in the current process. An empty list means it manages no
 * resource right now and does not make the microservice unhealthy.
 */
interface MicroserviceHealthContributor {

    /**
     * Stable name of this contributor, used when the check itself fails.
     */
    val name: String

    /**
     * Checks all active resources of this contributor.
     *
     * Must not block threads. The runtime cancels a check that exceeds its timeout and reports it as unhealthy.
     *
     * @return one result per active resource, or an empty list if none is active
     */
    suspend fun check(): List<MicroserviceHealthCheckResult>
}
