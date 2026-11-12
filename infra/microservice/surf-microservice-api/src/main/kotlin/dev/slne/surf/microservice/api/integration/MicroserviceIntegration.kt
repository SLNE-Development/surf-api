package dev.slne.surf.microservice.api.integration

/**
 * A resource the runtime starts before and stops after the microservice, such as the database or NATS.
 *
 * Implementations are loaded through [ServiceLoader][java.util.ServiceLoader] and need a public no-argument
 * constructor and an entry in `META-INF/services`, usually generated with `@AutoService`. Every integration on the
 * class path runs.
 *
 * Lifecycle, with integrations sorted by [order] and then by [name]:
 * 1. [start] in ascending order, before `Microservice.onBootstrap`.
 * 2. On shutdown, [stopAccepting] in descending order, before `Microservice.onDisable`.
 * 3. [stop] in descending order, after `Microservice.onDisable`.
 *
 * If the start of the microservice fails, the runtime runs [stopAccepting] and [stop] only for the integrations whose
 * [start] returned. An integration whose [start] throws must release what it acquired before it throws.
 * `Microservice.onDisable` runs only if `Microservice.onBootstrap` returned. Each call happens at most once per
 * integration and never concurrently with another lifecycle call.
 *
 * ```kotlin
 * @AutoService(MicroserviceIntegration::class)
 * class CacheIntegration : MicroserviceIntegration {
 *     override val name = "cache"
 *     override val order = MicroserviceIntegration.MESSAGING_ORDER + 10
 *
 *     override suspend fun start(context: MicroserviceIntegrationContext) {
 *         Cache.open(context.dataPath.resolve("cache.yml"), poolName = context.name)
 *     }
 *
 *     override suspend fun stop(context: MicroserviceIntegrationContext) {
 *         Cache.close()
 *     }
 * }
 * ```
 */
public interface MicroserviceIntegration {

    /**
     * Gets the name used in logs and as the tie-breaker between integrations with the same [order].
     */
    public val name: String

    /**
     * Gets the position in the start order. Lower values start first and stop last.
     *
     * The built-in integrations use [DATABASE_ORDER], [MESSAGING_ORDER] and [RELIABILITY_ORDER].
     */
    public val order: Int

    /**
     * Starts the resource.
     *
     * An exception aborts the start of the microservice, which then exits with code `1`.
     */
    public suspend fun start(context: MicroserviceIntegrationContext)

    /**
     * Stops accepting new work from outside, such as incoming requests, while outgoing work keeps working.
     *
     * Does nothing by default. Exceptions are logged and do not stop the remaining shutdown.
     */
    public suspend fun stopAccepting(context: MicroserviceIntegrationContext) {
    }

    /**
     * Releases the resource.
     *
     * Exceptions are logged and do not stop the remaining shutdown.
     */
    public suspend fun stop(context: MicroserviceIntegrationContext)

    public companion object {
        /**
         * The [order] of the database integration.
         */
        public const val DATABASE_ORDER: Int = 100

        /**
         * The [order] of the messaging integration (NATS). It starts after the database, so handlers find the
         * database ready.
         */
        public const val MESSAGING_ORDER: Int = 200

        /**
         * The [order] of the reliability integration (idempotent handlers and their events). It starts after the
         * database and messaging and stops before them.
         */
        public const val RELIABILITY_ORDER: Int = 300
    }
}
