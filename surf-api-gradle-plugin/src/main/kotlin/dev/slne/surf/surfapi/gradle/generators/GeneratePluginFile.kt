package dev.slne.surf.surfapi.gradle.generators

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePluginFile : DefaultTask() {
    @get:Input
    abstract val fileName: Property<String>

    @get:Input
    abstract val pluginFileJson: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val out = outputFile.get().asFile
        val json = pluginFileJson.get()

        if (json.isNotBlank()) {
            out.parentFile.mkdirs()
            out.writeText(json)
        } else {
            if (out.exists()) out.delete()
        }
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