package dev.slne.surf.microservice.gradle.plugin.core

import dev.slne.surf.microservice.gradle.plugin.CommonMicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class CoreMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : CommonMicroserviceExtension(objects) {
    override val module = objects.property<SurfMicroserviceCoreModule>()

    fun withCommon() {
        module.set(SurfMicroserviceCoreModule.COMMON)
    }

    fun withMicroservice() {
        module.set(SurfMicroserviceCoreModule.MICROSERVICE)
    }

    fun withRuntimeCommon() {
        module.set(SurfMicroserviceCoreModule.RUNTIME_COMMON)
    }

    fun withRuntimePaper() {
        module.set(SurfMicroserviceCoreModule.RUNTIME_PAPER)
    }

    fun withRuntimeVelocity() {
        module.set(SurfMicroserviceCoreModule.RUNTIME_VELOCITY)
    }
}