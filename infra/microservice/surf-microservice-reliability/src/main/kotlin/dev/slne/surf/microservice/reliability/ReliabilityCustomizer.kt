package dev.slne.surf.microservice.reliability

/**
 * Customizes the idempotent handlers of a microservice; implemented by the
 * [Microservice][dev.slne.surf.microservice.api.Microservice] subclass itself.
 *
 * Without this interface the reliability integration uses [ReliabilityConfig]'s defaults. The property is read once,
 * when the integration starts after the database and NATS and before `onBootstrap`.
 */
interface ReliabilityCustomizer {

    /**
     * Settings of the reliability integration.
     */
    val reliabilityConfig: ReliabilityConfig
        get() = ReliabilityConfig()
}
