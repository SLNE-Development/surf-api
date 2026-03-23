package dev.slne.surf.surfapi.compiler

import dev.slne.surf.surfapi.compiler.builder.BuilderIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class SurfCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = SurfCommandLineProcessor.PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        FirExtensionRegistrarAdapter.registerExtension(SurfFirExtensionRegistrar())

        IrGenerationExtension.registerExtension(TestFieldGeneration())
        IrGenerationExtension.registerExtension(BuilderIrGenerationExtension())
    }
}