package dev.slne.surf.microservice.api.integration

import dev.slne.surf.microservice.api.Microservice
import java.nio.file.Path

/**
 * What an integration knows about the microservice it serves.
 */
interface MicroserviceIntegrationContext {
    val microservice: Microservice

    /**
     * Validated [Microservice.name].
     */
    val name: String

    /**
     * [Microservice.dataPath]; integrations keep their configuration below it.
     */
    val dataPath: Path
}
