package dev.slne.surf.microservice.gradle.plugin.api

import dev.slne.surf.microservice.gradle.plugin.CommonMicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class ApiMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : CommonMicroserviceExtension(objects) {
    override val module = objects.property<SurfMicroserviceApiModule>()

    fun withCommon() {
        module.set(SurfMicroserviceApiModule.COMMON)
    }

    fun withMicroservice() {
        module.set(SurfMicroserviceApiModule.MICROSERVICE)
    }

    fun withRuntimeCommon() {
        module.set(SurfMicroserviceApiModule.RUNTIME_COMMON)
    }

    fun withRuntimePaper() {
        module.set(SurfMicroserviceApiModule.RUNTIME_PAPER)
    }

    fun withRuntimeVelocity() {
        module.set(SurfMicroserviceApiModule.RUNTIME_VELOCITY)
    }
}