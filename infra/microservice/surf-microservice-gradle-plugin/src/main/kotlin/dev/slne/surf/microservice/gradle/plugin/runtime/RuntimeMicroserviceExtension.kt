package dev.slne.surf.microservice.gradle.plugin.runtime

import dev.slne.surf.microservice.gradle.plugin.CommonMicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class RuntimeMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : CommonMicroserviceExtension(objects) {
    internal val coreModule = objects.property<SurfMicroserviceRuntimeModule>()

    fun withRuntimeCommon() {
        coreModule.set(SurfMicroserviceRuntimeModule.COMMON)
    }

    fun withRuntimeMicroservice() {
        coreModule.set(SurfMicroserviceRuntimeModule.MICROSERVICE)
    }

    fun withRuntimePaper() {
        coreModule.set(SurfMicroserviceRuntimeModule.PAPER)
    }

    fun withRuntimeVelocity() {
        coreModule.set(SurfMicroserviceRuntimeModule.VELOCITY)
    }
}