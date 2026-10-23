package dev.slne.surf.nats.gradle.tasks

import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.generator.GeneratorInput
import dev.slne.surf.nats.gradle.generator.SurfNatsGenerator
import dev.slne.surf.nats.gradle.generator.SurfNatsGeneratorException
import dev.slne.surf.nats.gradle.generator.TypeMapping
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Generates the Kotlin contracts of the `.proto` files in [sourceDirectories] into [outputDir].
 */
@CacheableTask
abstract class GenerateSurfNatsKotlinTask : ProtoInputsTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protoSources: FileTree
        get() = protoFiles(sourceDirectories)

    /**
     * Platform type mappings the plugin provides; [typeMappings] override them.
     */
    @get:Input
    abstract val defaultTypeMappings: ListProperty<TypeMapping>

    @get:Input
    abstract val typeMappings: ListProperty<TypeMapping>

    @get:Input
    abstract val visibility: Property<GeneratedVisibility>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val input = GeneratorInput(
            sourceDirs = sourceDirectories.files.toList(),
            protoPathDirs = protoPath(),
            typeMappings = typeMappings.get(),
            visibility = visibility.get(),
            defaultTypeMappings = defaultTypeMappings.get(),
        )
        try {
            SurfNatsGenerator().generate(input, outputDir.get().asFile)
        } catch (e: SurfNatsGeneratorException) {
            throw GradleException(e.message.orEmpty(), e)
        }
    }
}
