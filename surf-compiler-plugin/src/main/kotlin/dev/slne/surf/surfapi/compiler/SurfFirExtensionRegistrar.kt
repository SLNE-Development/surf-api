package dev.slne.surf.surfapi.compiler

import dev.slne.surf.surfapi.compiler.fir.TestFieldFirGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SurfFirExtensionRegistrar: FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::TestFieldFirGenerationExtension
    }
}