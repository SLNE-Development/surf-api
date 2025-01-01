package dev.slne.surf.surfapi.gradle.extensions

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.PlatformConfiguration
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import dev.slne.surf.surfapi.gradle.platform.config.paper.Paper
import dev.slne.surf.surfapi.gradle.platform.config.standalone.Standalone
import dev.slne.surf.surfapi.gradle.platform.config.velocity.Velocity

open class SurfApiExtension {
    internal var platform: SurfApiPlatform? = null
    internal var configuration: PlatformConfiguration? = null

    fun coreApi(configuration: Core.() -> Unit = {}) {
        checkConfiguration<Core>()
        platform = SurfApiPlatform.CORE

        if (this.configuration == null) this.configuration = Core()
        (this.configuration as Core).apply(configuration)
    }

    fun paperApi(configuration: Paper.() -> Unit = {}) {
        checkConfiguration<Paper>()
        platform = SurfApiPlatform.PAPER

        if (this.configuration == null) this.configuration = Paper()
        (this.configuration as Paper).apply(configuration)
    }

    fun velocityApi(configuration: Velocity.() -> Unit = {}) {
        checkConfiguration<Velocity>()
        platform = SurfApiPlatform.VELOCITY

        if (this.configuration == null) this.configuration = Velocity()
    }

    fun standaloneApi(configuration: Standalone.() -> Unit = {}) {
        checkConfiguration<Standalone>()
        platform = SurfApiPlatform.STANDALONE

        if (this.configuration == null) this.configuration = Standalone()
    }

    private inline fun <reified C : PlatformConfiguration> checkConfiguration() {
        require(this.configuration == null || this.configuration is C) { "Configuration already set to ${this.configuration?.javaClass?.simpleName}" }
    }
}