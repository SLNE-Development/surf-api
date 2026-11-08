package dev.slne.surf.microservice.api.integration

/**
 * A resource the runtime starts before and stops after the microservice, such as the database or NATS.
 *
 * Implementations are loaded through [ServiceLoader][java.util.ServiceLoader] and need a public no-argument
 * constructor and a `META-INF/services` entry, usually generated with `@AutoService`.
 *
 * Lifecycle, with integrations sorted by [order]:
 * 1. [start] in ascending order, before `Microservice.onBootstrap`.
 * 2. On shutdown, [stopAccepting] in descending order, before `Microservice.onDisable`.
 * 3. [stop] in descending order, after `Microservice.onDisable`.
 *
 * If the start of the microservice fails, the runtime runs [stopAccepting] and [stop] only for the integrations whose
 * [start] returned; `onDisable` runs only if `Microservice.onBootstrap` returned. Each call happens at most once per
 * integration and never concurrently with another lifecycle call.
 */
interface MicroserviceIntegration {

    /**
     * Name used in logs.
     */
    val name: String

    /**
     * Position in the start order; lower values start first and stop last.
     */
    val order: Int

    /**
     * Starts the resource. An exception aborts the start of the microservice.
     */
    suspend fun start(context: MicroserviceIntegrationContext)

    /**
     * Stops accepting new work from outside, such as incoming requests, while outgoing work keeps working.
     */
    suspend fun stopAccepting(context: MicroserviceIntegrationContext) {
    }

    /**
     * Releases the resource. Exceptions are logged and do not stop the remaining shutdown.
     */
    suspend fun stop(context: MicroserviceIntegrationContext)

    companion object {
        /**
         * [order] of the database integration.
         */
        const val DATABASE_ORDER = 100

        /**
         * [order] of the messaging integration (NATS); starts after the database so handlers find it ready.
         */
        const val MESSAGING_ORDER = 200

        /**
         * [order] of the reliability integration (idempotent handlers and their events); starts after the database
         * and messaging and stops before them.
         */
        const val RELIABILITY_ORDER = 300
    }
}
