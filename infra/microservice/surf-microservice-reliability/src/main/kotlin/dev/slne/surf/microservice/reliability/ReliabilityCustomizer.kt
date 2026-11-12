package dev.slne.surf.microservice.reliability

/**
 * Customizes the idempotent handlers of a microservice.
 *
 * Implement it on the [Microservice][dev.slne.surf.microservice.api.Microservice] subclass itself. Without this
 * interface the reliability integration uses the defaults of [ReliabilityConfig]. The property is read once, when the
 * integration starts after the database and NATS and before `onBootstrap`.
 *
 * ```kotlin
 * object PunishMicroservice : Microservice(), ReliabilityCustomizer {
 *     override val reliabilityConfig = ReliabilityConfig(cleanupInterval = 30.minutes)
 * }
 * ```
 */
public interface ReliabilityCustomizer {

    /**
     * Gets the settings of the reliability integration.
     */
    public val reliabilityConfig: ReliabilityConfig
        get() = ReliabilityConfig()
}
