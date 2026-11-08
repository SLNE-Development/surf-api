package dev.slne.surf.api.gradle.platform.paper

import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

abstract class AbstractPaperSurfExtension(objects: ObjectFactory) : CoreSurfExtension(objects) {
    val useCanvasMc = objects.property<Boolean>().convention(false)

    fun useCanvasMc() {
        useCanvasMc.set(true)
        useCanvasMc.finalizeValue()
    }
}