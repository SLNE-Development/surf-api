package dev.slne.surf.surfapi.gradle.platform.hytale

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.generators.pluginfiles.HytalePluginFile
import dev.slne.surf.surfapi.gradle.generators.pluginfiles.dto.HytalePluginFileDto
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfPluginWithPluginFile
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal class HytaleSurfPlugin :
    CommonSurfPluginWithPluginFile<HytaleSurfExtension, HytalePluginFile, HytalePluginFileDto>(
        "hytale",
        SurfApiPlatform.HYTALE,
        "manifest.json"
    ) {

    override val extensionClass = HytaleSurfExtension::class.java
    override val dtoSerializer = HytalePluginFileDto.serializer()

    init {
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
    }

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.HYTALE_SERVER)
        }
    }

    override fun createPluginFile(
        project: Project
    ) = project.extensions.create<HytalePluginFile>("hytalePluginFile").apply {
        group.convention("HYS")
        id.convention(project.name.lowercase())
        name.convention(project.provider { project.name })
        version.convention(project.provider { project.version.toString() })
        description.convention(project.provider { project.description ?: "" })
        authors.convention(mutableListOf("SLNE Development"))
        website.convention(project.providers.gradleProperty("url").orElse(""))
        serverVersion.convention("*")
        disabledByDefault.convention(false)
        includesAssetPack.convention(false)
        dependencies.convention(mutableMapOf("HYS:surf-api-hytale-server" to "*"))
        optionalDependencies.convention(mutableMapOf())
    }

    override fun createPluginFileDto(pluginFile: HytalePluginFile): HytalePluginFileDto {
        return HytalePluginFileDto.fromFile(pluginFile)
    }
}