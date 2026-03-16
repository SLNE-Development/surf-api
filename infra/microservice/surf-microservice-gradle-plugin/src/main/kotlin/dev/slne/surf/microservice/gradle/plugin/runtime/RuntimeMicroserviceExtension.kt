package dev.slne.surf.microservice.gradle.plugin.runtime

import dev.slne.surf.microservice.gradle.plugin.CommonMicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class RuntimeMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : CommonMicroserviceExtension() {
    override val module = objects.property<SurfMicroserviceRuntimeModule>()

    fun withRuntimeCommon() {
        module.set(SurfMicroserviceRuntimeModule.COMMON)
    }

    fun withRuntimeMicroservice() {
        module.set(SurfMicroserviceRuntimeModule.MICROSERVICE)
    }

    fun withRuntimePaper() {
        module.set(SurfMicroserviceRuntimeModule.PAPER)
    }

    fun withRuntimeVelocity() {
        module.set(SurfMicroserviceRuntimeModule.VELOCITY)
    }
}