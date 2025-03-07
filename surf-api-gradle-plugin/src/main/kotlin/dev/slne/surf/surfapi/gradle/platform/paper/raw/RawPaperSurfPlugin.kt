package dev.slne.surf.surfapi.gradle.platform.paper.raw

import dev.slne.surf.surfapi.gradle.platform.paper.AbstractPaperSurfPlugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

internal class RawPaperSurfPlugin : AbstractPaperSurfPlugin<RawPaperSurfExtension>("rawPaper") {
    override fun createExtension(objects: ObjectFactory, project: Project) = RawPaperSurfExtension(objects)
}