package dev.slne.surf.surfapi.gradle.generators

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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePluginFile : DefaultTask() {
    @get:Input
    abstract val fileName: Property<String>

    @get:Input
    abstract val pluginFileJson: Property<String>

    @get:OutputDirectory
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