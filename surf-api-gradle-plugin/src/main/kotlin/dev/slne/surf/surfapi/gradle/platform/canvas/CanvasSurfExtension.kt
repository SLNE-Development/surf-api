package dev.slne.surf.surfapi.gradle.platform.canvas

import dev.slne.surf.surfapi.gradle.platform.paper.plugin.PaperPluginSurfExtension
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class CanvasSurfExtension @Inject constructor(objects: ObjectFactory) :
    PaperPluginSurfExtension(objects)
