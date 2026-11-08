package dev.slne.surf.microservice.reliability.integration

/**
 * Holds the [ReliabilityRuntime] while [ReliabilityIntegration] is running.
 *
 * Reads are safe from any thread. Only the integration writes, and its lifecycle calls never run concurrently.
 */
internal object ReliabilityHolder {
    @Volatile
    private var current: ReliabilityRuntime? = null

    /**
     * @throws IllegalStateException if the reliability integration is not running
     */
    val runtime: ReliabilityRuntime
        get() = checkNotNull(current) {
            "The reliability integration is not running: it starts after the database and NATS and before " +
                "onBootstrap, and stops after onDisable"
        }

    fun install(runtime: ReliabilityRuntime) {
        current = runtime
    }

    fun uninstall(): ReliabilityRuntime? = current.also { current = null }
}
