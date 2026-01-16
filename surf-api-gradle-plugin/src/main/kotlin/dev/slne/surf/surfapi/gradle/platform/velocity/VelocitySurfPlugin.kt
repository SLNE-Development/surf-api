package dev.slne.surf.surfapi.gradle.platform.velocity

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.generators.pluginfiles.VelocityPluginFile
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.common.CommonSurfPluginWithPluginFile
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal class VelocitySurfPlugin :
    CommonSurfPluginWithPluginFile<VelocitySurfExtension, VelocityPluginFile>(
        "velocity",
        SurfApiPlatform.VELOCITY,
        "velocity-plugin.json"
    ) {

    override fun createExtension(objects: ObjectFactory, project: Project) =
        VelocitySurfExtension(project, objects)

    init {
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
    }

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.VELOCITY_API)
//            add("annotationProcessor", Constants.VELOCITY_API)
        }
    }

    override fun createPluginFile(project: Project) = VelocityPluginFile(project)
}