package dev.slne.surf.surfapi.gradle.platform.config

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform

abstract class PlatformConfiguration(internal val platform: SurfApiPlatform) {
    internal abstract fun validate()
}