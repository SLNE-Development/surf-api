package dev.slne.surf.surfapi.gradle.platform.standalone

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.core.AbstractCoreSurfPlugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

internal class StandaloneSurfPlugin :
    AbstractCoreSurfPlugin<StandaloneSurfExtension>("standalone", SurfApiPlatform.STANDALONE) {
    override fun createExtension(objects: ObjectFactory, project: Project) = StandaloneSurfExtension(objects)
}