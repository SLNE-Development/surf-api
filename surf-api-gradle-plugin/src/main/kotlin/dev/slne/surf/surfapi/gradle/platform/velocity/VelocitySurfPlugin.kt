package dev.slne.surf.surfapi.gradle.platform.velocity

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.core.AbstractCoreSurfPlugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal class VelocitySurfPlugin :
    AbstractCoreSurfPlugin<VelocitySurfExtension>("velocity", SurfApiPlatform.VELOCITY) {
    override fun createExtension(objects: ObjectFactory) = VelocitySurfExtension(objects)

    init {
        "it.unimi.dsi.fastutil" relocatesTo "fastutil"
    }

    override fun Project.configure0() {
        dependencies {
            add(COMPILE_ONLY, Constants.VELOCITY_API)
            add("annotationProcessor", Constants.VELOCITY_API)
        }
    }
}