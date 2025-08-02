package dev.slne.surf.surfapi.gradle.platform.paper

import dev.slne.surf.surfapi.gradle.SurfCloudModules
import dev.slne.surf.surfapi.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory

abstract class AbstractPaperSurfExtension(objects: ObjectFactory) : CoreSurfExtension(objects) {
    fun withCloudClientPaper() {
        cloudModule.set(SurfCloudModules.CLIENT_PAPER)
    }
}