package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import java.nio.file.Path

internal data class RuntimeIntegrationContext(
    override val microservice: Microservice,
    override val name: String,
    override val dataPath: Path,
) : MicroserviceIntegrationContext
