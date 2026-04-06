package dev.slne.surf.api.gradle.platform.velocity

import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class VelocitySurfExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory,
) : CoreSurfExtension(objects)