package dev.slne.surf.surfapi.gradle.extensions

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.PlatformConfiguration
import dev.slne.surf.surfapi.gradle.platform.config.core.Core
import dev.slne.surf.surfapi.gradle.platform.config.paper.Paper
import dev.slne.surf.surfapi.gradle.platform.config.standalone.Standalone
import dev.slne.surf.surfapi.gradle.platform.config.velocity.Velocity
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject


open class SurfApiExtension @Inject constructor(private val objects: ObjectFactory) {
    internal val platform = objects.property<SurfApiPlatform>()
    internal var configuration: PlatformConfiguration? = null

    private fun ensureSinglePlatform(newPlatform: SurfApiPlatform) {
        check(!platform.isPresent) { "Platform already set to ${platform.get()}, cannot set to $newPlatform" }

        platform.set(newPlatform)
        platform.finalizeValue()
    }

    private inline fun <reified P : PlatformConfiguration> configurePlatform(configuration: Action<P>) {
        if (this.configuration == null) {
            this.configuration = objects.newInstance<P>()
        }

        configuration.execute(this.configuration as P)
    }

    fun coreApi(configuration: Action<Core> = Action {}) {
        ensureSinglePlatform(SurfApiPlatform.CORE)
        configurePlatform(configuration)
    }

    fun paperApi(configuration: Action<Paper> = Action {}) {
        ensureSinglePlatform(SurfApiPlatform.PAPER)
        configurePlatform(configuration)
    }

    fun velocityApi(configuration: Action<Velocity> = Action {}) {
        ensureSinglePlatform(SurfApiPlatform.VELOCITY)
        configurePlatform(configuration)
    }

    fun standaloneApi(configuration: Action<Standalone> = Action {}) {
        ensureSinglePlatform(SurfApiPlatform.STANDALONE)
        configurePlatform(configuration)
    }

    companion object {
        fun Project.surfApi() = extensions.create<SurfApiExtension>("surfApi")
    }
}