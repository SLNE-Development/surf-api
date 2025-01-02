package dev.slne.surf.surfapi.gradle.platform.config.standalone

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class Standalone @Inject constructor(objects: ObjectFactory) :
    Core(objects, SurfApiPlatform.STANDALONE)