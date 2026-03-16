package dev.slne.surf.microservice.gradle.plugin.core

import dev.slne.surf.microservice.gradle.plugin.MicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class CoreMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : MicroserviceExtension(objects) {
    override val module = objects.property<SurfMicroserviceCoreModule>()

    fun withCommon() {
        module.set(SurfMicroserviceCoreModule.COMMON)
    }
}