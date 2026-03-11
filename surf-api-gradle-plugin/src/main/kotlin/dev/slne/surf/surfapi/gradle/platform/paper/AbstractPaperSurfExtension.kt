package dev.slne.surf.surfapi.gradle.platform.paper

import dev.slne.surf.surfapi.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

abstract class AbstractPaperSurfExtension(objects: ObjectFactory) : CoreSurfExtension(objects) {
    val useCanvasMc = objects.property<Boolean>().convention(false)

    fun useCanvasMc() {
        useCanvasMc.set(true)
        useCanvasMc.finalizeValue()
    }
}