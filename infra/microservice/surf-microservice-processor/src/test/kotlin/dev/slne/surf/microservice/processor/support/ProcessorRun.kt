package dev.slne.surf.microservice.processor.support

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import dev.slne.surf.microservice.processor.MicroserviceSymbolProcessorProvider
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * Result of compiling sources with KSP2 and the microservice processor.
 */
@OptIn(ExperimentalCompilerApi::class)
class ProcessorRun private constructor(
    val exitCode: KotlinCompilation.ExitCode,
    val messages: String,
    private val resourceDirectory: File,
) {

    /**
     * Returns the content of the generated resource, or `null` if the processor did not write it.
     */
    fun resource(path: String): String? =
        resourceDirectory.resolve(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    companion object {
        fun compile(
            vararg sources: SourceFile,
            additionalProviders: List<SymbolProcessorProvider> = emptyList(),
        ): ProcessorRun {
            val compilation = KotlinCompilation().apply {
                this.sources = sources.toList()
                configureKsp {
                    symbolProcessorProviders += additionalProviders
                    symbolProcessorProviders += MicroserviceSymbolProcessorProvider()
                }
            }
            val result = compilation.compile()
            return ProcessorRun(result.exitCode, result.messages, compilation.kspSourcesDir.resolve("resources"))
        }
    }
}
