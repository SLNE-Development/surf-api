package dev.slne.surf.microservice.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Creates the [MicroserviceSymbolProcessor].
 *
 * KSP loads this provider through the `META-INF/services` entry for `SymbolProcessorProvider`. Application code does
 * not use it.
 */
class MicroserviceSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        MicroserviceSymbolProcessor(environment.codeGenerator, environment.logger)
}
