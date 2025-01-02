package dev.slne.surf.surfapi.gradle.platform.config

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import org.gradle.api.model.ObjectFactory

abstract class PlatformConfiguration(
    internal val objects: ObjectFactory,
    internal val platform: SurfApiPlatform,
) {
    internal abstract fun validate()
}