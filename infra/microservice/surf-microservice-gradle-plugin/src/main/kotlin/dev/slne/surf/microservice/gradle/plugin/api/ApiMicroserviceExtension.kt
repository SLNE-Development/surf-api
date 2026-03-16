package dev.slne.surf.microservice.gradle.plugin.api

import dev.slne.surf.microservice.gradle.plugin.MicroserviceExtension
import dev.slne.surf.microservice.gradle.plugin.client.SurfMicroserviceModule
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

surfMicroserviceAPi {
    withMicroservice()
    withMicroserviceAPi()

    withMicroserv iceCore()
}

abstract class ApiMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : MicroserviceExtension(objects) {

    fun withCommon() {
        module.set(SurfMicroserviceModule.COMMON)
    }

    fun withMicroservice() {
        module.set(SurfMicroserviceModule.MICROSERVICE)
    }

    fun withRuntimeCommon() {
        module.set(SurfMicroserviceModule.RUNTIME_COMMON)
    }

    fun withRuntimePaper() {
        module.set(SurfMicroserviceModule.RUNTIME_PAPER)
    }

    fun withRuntimeVelocity() {
        module.set(SurfMicroserviceModule.RUNTIME_VELOCITY)
    }
}