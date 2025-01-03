package dev.slne.surf.surfapi.gradle.platform.paper.raw

import dev.slne.surf.surfapi.gradle.platform.paper.AbstractPaperSurfExtension
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class RawPaperSurfExtension @Inject constructor(objects: ObjectFactory) :
    AbstractPaperSurfExtension(objects) {
}