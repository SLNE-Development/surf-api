package dev.slne.surf.surfapi.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.util.Collections.emptyList

@OptIn(ExperimentalCompilerApi::class)
class SurfCommandLineProcessor: CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = emptyList()

    companion object {
        const val PLUGIN_ID = "dev.slne.surf.surfapi.compiler"
    }
}