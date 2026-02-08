package dev.slne.surf.surfapi.gradle.generators.pluginfiles.dto

import dev.slne.surf.surfapi.gradle.generators.pluginfiles.HytalePluginFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HytalePluginFileDto(
    @SerialName("Group")
    val group: String,

    @SerialName("Id")
    val id: String? = null,

    @SerialName("Name")
    val name: String,

    @SerialName("Version")
    val version: String,

    @SerialName("Description")
    val description: String,

    @SerialName("Authors")
    val authors: List<AuthorDto>,

    @SerialName("Website")
    val website: String,

    @SerialName("ServerVersion")
    val serverVersion: String,

    @SerialName("DisabledByDefault")
    val disabledByDefault: Boolean,

    @SerialName("Main")
    val main: String,

    @SerialName("IncludesAssetPack")
    val includesAssetPack: Boolean,

    @SerialName("Dependencies")
    val dependencies: Map<String, String>,

    @SerialName("OptionalDependencies")
    val optionalDependencies: Map<String, String>,
) {
    @Serializable
    data class AuthorDto(
        val name: String,
    )

    companion object {
        fun fromFile(file: HytalePluginFile) = HytalePluginFileDto(
            group = file.group.get(),
            name = file.name.get(),
            version = file.version.get(),
            description = file.description.get(),
            authors = file.authors.getOrElse(emptyList()).map { AuthorDto(it) },
            website = file.website.get(),
            serverVersion = file.serverVersion.get(),
            disabledByDefault = file.disabledByDefault.get(),
            main = file.main.get(),
            includesAssetPack = file.includesAssetPack.get(),
            dependencies = file.dependencies.get(),
            optionalDependencies = file.optionalDependencies.get()
        )
    }
}