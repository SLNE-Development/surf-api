package dev.slne.surf.microservice.processor.support

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Stands in for another processor that generates a Kotlin file in the first round.
 */
class SourceGeneratingProcessorProvider(
    private val packageName: String,
    private val fileName: String,
    private val code: String,
) : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = object : SymbolProcessor {
        private var generated = false

        override fun process(resolver: Resolver): List<KSAnnotated> {
            if (!generated) {
                environment.codeGenerator.createNewFile(Dependencies(aggregating = false), packageName, fileName)
                    .bufferedWriter()
                    .use { it.write(code.trimIndent()) }
                generated = true
            }
            return emptyList()
        }
    }
}
