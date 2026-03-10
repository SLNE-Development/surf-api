package dev.slne.surf.surfapi.gradle.platform.paper.plugin

internal class PaperPluginSurfPlugin :
    AbstractPaperPluginSurfPlugin<PaperPluginSurfExtension>("paperPlugin") {

    override val extensionClass = PaperPluginSurfExtension::class.java
}