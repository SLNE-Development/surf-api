package dev.slne.surf.surfapi.gradle.platform.velocity

import dev.slne.surf.surfapi.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class VelocitySurfExtension @Inject constructor(objects: ObjectFactory) :
    CoreSurfExtension(objects) {

}