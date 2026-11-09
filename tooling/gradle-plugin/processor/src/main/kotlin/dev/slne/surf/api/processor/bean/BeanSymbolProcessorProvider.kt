package dev.slne.surf.api.processor.bean

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/** Hands KSP a [BeanSymbolProcessor] for the compilation it is about to run. */
class BeanSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return BeanSymbolProcessor(environment)
    }
}
