package dev.slne.surf.surfapi.gradle.platform.config.core

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.config.PlatformConfiguration
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

open class Core @Inject constructor(
    objects: ObjectFactory,
    platform: SurfApiPlatform = SurfApiPlatform.CORE,
) : PlatformConfiguration(objects, platform) {
    val authors = objects.listProperty<String>().convention(mutableListOf("SLNE Development Team"))

    override fun validate() {
        validate0()
    }

    internal open fun validate0() {
    }
}