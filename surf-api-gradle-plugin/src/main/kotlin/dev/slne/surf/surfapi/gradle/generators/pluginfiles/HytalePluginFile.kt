package dev.slne.surf.surfapi.gradle.generators.pluginfiles

import dev.slne.surf.surfapi.gradle.platform.invalidPluginFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

@Serializable(with = HytalePluginFileSerializer::class)
class HytalePluginFile(project: Project, objects: ObjectFactory) : CommonPluginFile() {
    @Input
    @SerialName("Group")
    var group: String = "HYS"

    @Input
    @SerialName("Id")
    var id: String? = null

    @Input
    @SerialName("Name")
    var name: String? = null

    @Input
    @SerialName("Version")
    var version: String? = null

    @Input
    @SerialName("Description")
    @Optional
    var description: String = ""

    @Input
    @SerialName("Authors")
    var authors: List<Author> = mutableListOf()

    @Input
    @SerialName("Website")
    @Optional
    var website: String = ""

    @Input
    @SerialName("ServerVersion")
    var serverVersion: String = "*"

    @Input
    @SerialName("DisabledByDefault")
    var disabledByDefault: Boolean = false

    @Input
    @SerialName("Main")
    var main: String? = null

    @Input
    @SerialName("IncludesAssetPack")
    var includesAssetPack: Boolean = false

    @Input
    @SerialName("Dependencies")
    val dependencies: MutableMap<String, String> = mutableMapOf("surf-api-hytale-server" to "*")

    @Input
    @SerialName("OptionalDependencies")
    val optionalDependencies: MutableMap<String, String> = mutableMapOf()

    @Serializable
    data class Author(@SerialName("Name") @Input val name: String)

    override fun isApplied(): Boolean {


        return name != null && main != null
    }

    override fun setDefaults(project: Project) {
        id = project.name
        name = project.name
        version = project.version.toString()
        description = project.description ?: ""
        website = project.findProperty("url") as String? ?: ""
    }

    override fun validate() {
        val id = name ?: invalidPluginFile("Plugin name not set")

        if (version.isNullOrBlank()) invalidPluginFile("Plugin version not set")
        if (main.isNullOrBlank()) invalidPluginFile("Main class not set")

        for ((dependency, version) in optionalDependencies) {
            if (dependency.isBlank()) invalidPluginFile("Dependency id not set")
            if (version.isBlank()) invalidPluginFile("Dependency '$dependency' version not set")
        }
    }
}

object HytalePluginFileSerializer : KSerializer<HytalePluginFile> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HytalePluginFile") {
        element<String>("Group")
        element<String>("Name")
        element<String>("Version")
        element<String>("Description")
        element<List<HytalePluginFile.Author>>("Authors")
        element<String>("Website")
        element<String>("ServerVersion")
        element<Boolean>("DisabledByDefault")
        element<String>("Main")
        element<Boolean>("IncludesAssetPack")
        element<Map<String, String>>("Dependencies")
        element<Map<String, String>>("PluginDependencies")
    }

    override fun serialize(encoder: Encoder, value: HytalePluginFile) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.group ?: "")
            encodeStringElement(descriptor, 1, value.name ?: "")
            encodeStringElement(descriptor, 2, value.version ?: "")
            encodeStringElement(descriptor, 3, value.description ?: "")
            encodeSerializableElement(
                descriptor,
                4,
                ListSerializer(HytalePluginFile.Author.serializer()),
                value.authors
            )
            encodeStringElement(descriptor, 5, value.website ?: "")
            encodeStringElement(descriptor, 6, value.serverVersion ?: "")
            encodeBooleanElement(descriptor, 7, value.disabledByDefault)
            encodeStringElement(descriptor, 8, value.main ?: "")
            encodeBooleanElement(descriptor, 9, value.includesAssetPack)
            encodeSerializableElement(
                descriptor,
                10,
                MapSerializer(String.serializer(), String.serializer()),
                value.dependencies
            )
            encodeSerializableElement(
                descriptor,
                11,
                MapSerializer(String.serializer(), String.serializer()),
                value.optionalDependencies
            )
        }
    }

    override fun deserialize(decoder: Decoder): HytalePluginFile {
        throw NotImplementedError("Deserialization is not supported for HytalePluginFile")
    }
}