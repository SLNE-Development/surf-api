package dev.slne.surf.microservice.gradle.plugin.client

import dev.slne.surf.microservice.gradle.plugin.MicroserviceExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class ClientMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) : MicroserviceExtension(objects) {
    override val module = objects.property<SurfMicroserviceModule>()

    fun withClientCommon() {
        module.set(SurfMicroserviceModule.CLIENT_COMMON)
    }

    fun withClientPaper() {
        module.set(SurfMicroserviceModule.CLIENT_PAPER)
    }

    fun withClientVelocity() {
        module.set(SurfMicroserviceModule.CLIENT_VELOCITY)
    }
}