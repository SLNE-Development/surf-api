package dev.slne.surf.surfapi.gradle.generators.pluginfiles.dto

import dev.slne.surf.surfapi.gradle.generators.pluginfiles.VelocityPluginFile
import kotlinx.serialization.Serializable


@Serializable
data class VelocityPluginFileDto(
    val id: String? = null,
    val main: String? = null,
    val name: String? = null,
    val version: String? = null,
    val description: String? = null,
    val url: String? = null,
    val authors: List<String>? = null,
    val dependencies: List<DependencyDto>? = null,
) {
    @Serializable
    data class DependencyDto(
        val id: String,
        val optional: Boolean,
    )


    companion object {
        fun fromFile(file: VelocityPluginFile) = VelocityPluginFileDto(
            id = file.id.orNull,
            main = file.main.orNull,
            name = file.name.orNull,
            version = file.version.orNull,
            description = file.description.orNull,
            url = file.url.orNull,
            authors = file.authors.orNull,
            dependencies = file.pluginDependencies
                .filter { it.enabled.get() }
                .map { DependencyDto(it.name, it.optional.get()) }
        )
    }
}


