package dev.slne.surf.microservice.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Creates the [MicroserviceSymbolProcessor]; registered as a KSP service.
 */
class MicroserviceSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        MicroserviceSymbolProcessor(environment.codeGenerator, environment.logger)
}
