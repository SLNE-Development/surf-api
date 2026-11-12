package dev.slne.surf.microservice.api.integration

import dev.slne.surf.microservice.api.Microservice
import java.nio.file.Path

/**
 * Describes the microservice that an integration serves.
 *
 * The runtime creates one context before the first integration starts and passes the same instance to every
 * lifecycle call of every [MicroserviceIntegration].
 */
public interface MicroserviceIntegrationContext {
    /**
     * Gets the running microservice.
     *
     * An integration can check it for an optional interface that the microservice class implements to customize the
     * integration, such as `DatabaseCustomizer`.
     */
    public val microservice: Microservice

    /**
     * Gets the [Microservice.name], read once at start and validated.
     */
    public val name: String

    /**
     * Gets the [Microservice.dataPath], read once at start. Integrations keep their configuration below it.
     */
    public val dataPath: Path
}
