package dev.slne.surf.surfapi.gradle.platform.config.core

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.PlatformConfiguration

open class Core(platform: SurfApiPlatform = SurfApiPlatform.CORE) :
    PlatformConfiguration(platform) {
    var authors: List<String> = listOf("SLNE Development Team")

    override fun validate() {
        validate0()
    }

    internal open fun validate0() {
    }
}