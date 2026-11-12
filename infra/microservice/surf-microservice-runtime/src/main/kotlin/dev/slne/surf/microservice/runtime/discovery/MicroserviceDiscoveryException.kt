package dev.slne.surf.microservice.runtime.discovery

import java.io.Serial

/**
 * Thrown when the runtime cannot determine or create the microservice to run.
 */
internal class MicroserviceDiscoveryException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = -2_903_118_462_550_917_204L
    }
}
