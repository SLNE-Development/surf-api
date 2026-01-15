package dev.slne.surf.surfapi.gradle.platform.hytale

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.generators.pluginfiles.HytalePluginFile
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfPluginWithPluginFile
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal class HytaleSurfPlugin :
    CommonSurfPluginWithPluginFile<HytaleSurfExtension, HytalePluginFile>(
        "hytale",
        SurfApiPlatform.HYTALE,
        "manifest.json"
    ) {

    override fun createExtension(objects: ObjectFactory, project: Project) =
        HytaleSurfExtension(project, objects)

    init {
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
    }

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.HYTALE_SERVER)
        }
    }

    override fun createPluginFile(project: Project) = HytalePluginFile(project)
}