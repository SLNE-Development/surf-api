package dev.slne.surf.surfapi.gradle.platform.paper.raw

import dev.slne.surf.surfapi.gradle.platform.paper.AbstractPaperSurfPlugin

internal class RawPaperSurfPlugin : AbstractPaperSurfPlugin<RawPaperSurfExtension>("rawPaper") {
    override val extensionClass = RawPaperSurfExtension::class.java
}