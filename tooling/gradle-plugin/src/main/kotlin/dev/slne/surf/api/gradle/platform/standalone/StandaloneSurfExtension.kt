package dev.slne.surf.api.gradle.platform.standalone

import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class StandaloneSurfExtension @Inject constructor(objects: ObjectFactory) :
    CoreSurfExtension(objects)