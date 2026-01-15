package dev.slne.surf.surfapi.gradle.platform.hytale

import dev.slne.surf.surfapi.gradle.platform.core.CoreSurfExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class HytaleSurfExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory,
) : CoreSurfExtension(objects)