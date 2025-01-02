package dev.slne.surf.surfapi.gradle.platform.config.velocity

import dev.slne.surf.surfapi.gradle.generated.Constants
import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import javax.inject.Inject

open class Velocity @Inject constructor(objects: ObjectFactory) : Core(objects, SurfApiPlatform.VELOCITY)

internal fun Project.applyVelocityConfiguration(configuration: Velocity) {
    dependencies {
        add(COMPILE_ONLY, Constants.VELOCITY_API)
        add("kapt", Constants.VELOCITY_API)
    }
}