package dev.slne.surf.surfapi.gradle.platform.standalone

import dev.slne.surf.surfapi.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class StandaloneSurfExtension @Inject constructor(objects: ObjectFactory) :
    CoreSurfExtension(objects)