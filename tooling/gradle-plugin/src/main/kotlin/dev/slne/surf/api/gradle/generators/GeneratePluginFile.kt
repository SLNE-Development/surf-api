package dev.slne.surf.api.gradle.generators

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Writes [pluginFileJson] to [fileName] below [outputDir].
 *
 * The task owns [outputDir]. It writes nothing when the content or the file name is absent or
 * blank, which is how a plugin that generates no main class ends up without the file.
 */
@CacheableTask
abstract class GeneratePluginFile : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val pluginFileJson: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val fileName: Property<String>

    @TaskAction
    fun generate() {
        val directory = outputDir.get().asFile
        directory.deleteRecursively()

        val json = pluginFileJson.orNull
        val name = fileName.orNull
        if (json.isNullOrBlank() || name.isNullOrBlank()) return

        val outFile = directory.resolve(name)
        outFile.parentFile.mkdirs()
        outFile.writeText(json)
    }

    companion object {
        val json = Json {
            isLenient = true
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}