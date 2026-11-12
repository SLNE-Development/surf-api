package dev.slne.surf.microservice.api.health

/**
 * Contributes readiness checks to the `health` command of a microservice.
 *
 * Implementations are loaded through [ServiceLoader][java.util.ServiceLoader] and need a public no-argument
 * constructor and an entry in `META-INF/services`, usually generated with `@AutoService`. The runtime loads them on
 * the first `health` invocation and runs all contributors concurrently. The microservice is healthy when no result is
 * [MicroserviceHealthStatus.UNHEALTHY]. Otherwise `health` fails and `microservicectl health` exits with code `1`.
 *
 * A contributor reports only resources that are active in the current process. An empty list means it manages no
 * resource right now and does not make the microservice unhealthy.
 *
 * ```kotlin
 * @AutoService(MicroserviceHealthContributor::class)
 * class CacheHealthContributor : MicroserviceHealthContributor {
 *     override val name = "cache"
 *
 *     override suspend fun check(): List<MicroserviceHealthCheckResult> {
 *         val cache = Cache.currentOrNull() ?: return emptyList()
 *         val status = if (cache.isConnected) MicroserviceHealthStatus.HEALTHY else MicroserviceHealthStatus.UNHEALTHY
 *         return listOf(
 *             MicroserviceHealthCheckResult(
 *                 component = name,
 *                 status = status,
 *                 message = "Cache is disconnected".takeIf { status == MicroserviceHealthStatus.UNHEALTHY },
 *             )
 *         )
 *     }
 * }
 * ```
 */
public interface MicroserviceHealthContributor {

    /**
     * Gets the stable name of this contributor, used as the component of the result when the check itself fails.
     */
    public val name: String

    /**
     * Checks all active resources of this contributor.
     *
     * Must not block threads. The runtime cancels a check that takes longer than 3 seconds, and reports a timeout or
     * an exception as one unhealthy result named after [name].
     *
     * @return one result per active resource, or an empty list if none is active
     */
    public suspend fun check(): List<MicroserviceHealthCheckResult>
}
