package dev.slne.surf.surfapi.gradle.generators

import dev.slne.surf.surfapi.gradle.generators.pluginfiles.CommonPluginFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GeneratePluginFile : DefaultTask() {
    @get:Input
    abstract val fileName: Property<String>

    @get:Nested
    abstract val pluginFile: Property<CommonPluginFile>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val pluginFile = pluginFile.get()
        if (pluginFile.isApplied()) {
            val encoded = json.encodeToString(pluginFile)
            outputDirectory.file(fileName).get().asFile.writeText(encoded)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    companion object {
        private val json = Json {
            isLenient = true
            prettyPrint = true
            prettyPrintIndent = "  "
        }

        @OptIn(InternalSerializationApi::class)
        class NamedDomainObjectContainerSerializer<T : Any>(
            private val dataSerializer: KSerializer<T>,
        ) : KSerializer<NamedDomainObjectContainer<T>> {
            override val descriptor: SerialDescriptor = SerialDescriptor(
                "dev.slne.surf.surfapi.gradle.generators.GeneratePluginFile.NamedDomainObjectContainerSerializer",
                dataSerializer.descriptor
            )

            override fun serialize(
                encoder: Encoder,
                value: NamedDomainObjectContainer<T>,
            ) {
                val values = value.toList()
                encoder.encodeSerializableValue(ListSerializer(dataSerializer), values)
            }

            override fun deserialize(decoder: Decoder): NamedDomainObjectContainer<T> {
                throw UnsupportedOperationException("Deserialization is not supported")
            }
        }
    }
}