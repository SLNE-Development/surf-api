package dev.slne.surf.api.gradle.platform.paper.raw

import dev.slne.surf.api.gradle.platform.paper.AbstractPaperSurfPlugin

internal class RawPaperSurfPlugin : AbstractPaperSurfPlugin<RawPaperSurfExtension>("rawPaper") {
    override val extensionClass = RawPaperSurfExtension::class.java
}