package dev.slne.surf.microservice.gradle.plugin.core

import dev.slne.surf.microservice.gradle.plugin.CommonMicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class CoreMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : CommonMicroserviceExtension(objects) {
    internal val coreModule = objects.property<SurfMicroserviceCoreModule>()

    fun withCommon() {
        coreModule.set(SurfMicroserviceCoreModule.COMMON)
    }

    fun withMicroservice() {
        coreModule.set(SurfMicroserviceCoreModule.MICROSERVICE)
    }

    fun withRuntimeCommon() {
        coreModule.set(SurfMicroserviceCoreModule.RUNTIME_COMMON)
    }

    fun withRuntimePaper() {
        coreModule.set(SurfMicroserviceCoreModule.RUNTIME_PAPER)
    }

    fun withRuntimeVelocity() {
        coreModule.set(SurfMicroserviceCoreModule.RUNTIME_VELOCITY)
    }
}