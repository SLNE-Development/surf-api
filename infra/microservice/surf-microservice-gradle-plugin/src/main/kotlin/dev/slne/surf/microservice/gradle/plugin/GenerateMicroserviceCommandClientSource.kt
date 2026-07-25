package dev.slne.surf.microservice.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
internal abstract class GenerateMicroserviceCommandClientSource : DefaultTask() {
    @get:Input
    abstract val sourceText: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val target = outputFile.get().asFile

        target.parentFile?.let { parent ->
            if (!parent.mkdirs() && !parent.isDirectory) {
                throw GradleException(
                    "Could not create command client source directory: ${parent.absolutePath}"
                )
            }
        }

        target.writeText(sourceText.get(), Charsets.UTF_8)
    }
}