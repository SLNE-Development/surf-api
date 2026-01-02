package dev.slne.surf.surfapi.gradle.generators.pluginfiles

import dev.slne.surf.surfapi.gradle.generators.GeneratePluginFile.Companion.NamedDomainObjectContainerSerializer
import dev.slne.surf.surfapi.gradle.generators.pluginfiles.VelocityPluginFile.Dependency
import dev.slne.surf.surfapi.gradle.platform.invalidPluginFile
import dev.slne.surf.surfapi.gradle.platform.velocity.VelocitySurfExtension
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.findByType
import org.intellij.lang.annotations.Pattern
import org.intellij.lang.annotations.RegExp

@RegExp
private const val ID_REGEX = "[a-z][a-z0-9-_]{0,63}"

@Serializable(with = VelocityPluginFileSerializer::class)
class VelocityPluginFile(project: Project) : CommonPluginFile() {
    @Transient
    private val idRegex = ID_REGEX.toRegex()

    @Pattern(ID_REGEX)
    @Input
    @Optional
    var id: String? = null

    @Input
    @Optional
    var main: String? = null

    @Input
    @Optional
    var name: String? = null

    @Input
    @Optional
    var version: String? = null

    @Input
    @Optional
    var description: String? = null

    @Input
    @Optional
    var url: String? = null

    @Input
    @Optional
    var authors: List<String>? = null


    @Serializable(with = NamedDomainObjectContainerSerializer::class)
    @Nested
    @Optional
    var pluginDependencies: NamedDomainObjectContainer<Dependency> =
        project.container(Dependency::class.java).apply {
            register("surf-api-velocity") {
                optional = false
            }

            project.extensions.findByType<VelocitySurfExtension>()?.let { extension ->
                if (extension.cloudModule.isPresent) {
                    register("surf-cloud-velocity") {
                        optional = false
                    }
                }

                if (extension.coreModule.isPresent) {
                    register("surf-core-velocity") {
                        optional = false
                    }
                }

                if (extension.withSurfRedis.isPresent && !extension.surfRedisRelocation.isPresent) {
                    register("surf-redis-velocity") {
                        optional = false
                    }
                }
            }
        }

    @Serializable
    data class Dependency(@SerialName("id") @Input val name: String) {
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault
        @Input
        var optional: Boolean = false
    }

    override fun isApplied(): Boolean {
        return id != null && main != null
    }

    override fun setDefaults(project: Project) {
        id = project.name
        version = project.version.toString()
        description = project.description
        url = project.findProperty("url") as String?
    }

    override fun validate() {
        val id = id ?: invalidPluginFile("Plugin id not set")
        if (!(idRegex.matches(id))) invalidPluginFile("Invalid plugin id! Should match $idRegex")

        if (version.isNullOrBlank()) invalidPluginFile("Plugin version not set")
        if (main.isNullOrBlank()) invalidPluginFile("Main class not set")

        for (dependency in pluginDependencies) {
            if (dependency.name.isBlank()) invalidPluginFile("Dependency id not set")
        }
    }
}

object VelocityPluginFileSerializer : KSerializer<VelocityPluginFile> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("VelocityPluginFile") {
        element<String>("id", isOptional = true)
        element<String>("main", isOptional = true)
        element<String>("name", isOptional = true)
        element<String>("version", isOptional = true)
        element<String>("description", isOptional = true)
        element<String>("url", isOptional = true)
        element<List<String>>("authors", isOptional = true)
        element<Array<Dependency>>("dependencies", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: VelocityPluginFile) {
        encoder.encodeStructure(descriptor) {
            value.id?.let { encodeStringElement(descriptor, 0, it) }
            value.main?.let { encodeStringElement(descriptor, 1, it) }
            value.name?.let { encodeStringElement(descriptor, 2, it) }
            value.version?.let { encodeStringElement(descriptor, 3, it) }
            value.description?.let { encodeStringElement(descriptor, 4, it) }
            value.url?.let { encodeStringElement(descriptor, 5, it) }
            value.authors?.let {
                encodeSerializableElement(
                    descriptor,
                    6,
                    ListSerializer(String.serializer()),
                    it
                )
            }

            val dep = value.pluginDependencies
            val namer = dep.namer

            dep.associateBy { namer.determineName(it) }

            encodeSerializableElement(
                descriptor,
                7,
                ArraySerializer(Dependency.serializer()),
                value.pluginDependencies.toTypedArray()
            )
        }
    }

    override fun deserialize(decoder: Decoder): VelocityPluginFile {
        throw UnsupportedOperationException("Deserialization is not supported")
    }
}