package dev.slne.surf.surfapi.gradle.generators

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePluginFile : DefaultTask() {
    @get:Input
    abstract val pluginFileJson: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val fileName: Property<String>

    @TaskAction
    fun generate() {
        val outFile = outputDir.file(fileName).get().asFile
        val json = pluginFileJson.get()

        if (json.isBlank()) {
            if (outFile.exists()) outFile.delete()
            return
        }

        outFile.parentFile.mkdirs()
        outFile.writeText(json)
    }

    @OptIn(ExperimentalSerializationApi::class)
    companion object {
        val json = Json {
            isLenient = true
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}