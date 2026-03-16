package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.plugin.client.SurfMicroserviceModule
import dev.slne.surf.microservice.gradle.plugin.utils.RabbitModule
import dev.slne.surf.microservice.gradle.plugin.utils.RabbitModuleSettings
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

abstract class MicroserviceExtension(objects: ObjectFactory) {
    internal val module = objects.property<SurfMicroserviceModule>()
    internal val rabbitSettings = objects.property<RabbitModuleSettings>()

    fun withRabbitModule(
        rabbitModule: RabbitModule,
        applyRabbitServerRuntimeDependency: Boolean = false
    ) {
        this.rabbitSettings.set(
            RabbitModuleSettings(
                rabbitModule,
                applyRabbitServerRuntimeDependency
            )
        )
    }

    fun withMicroserviceCommon() {
        module.set(SurfMicroserviceModule.COMMON)
        module.finalizeValue()
    }

    fun withMicroserviceMircroservice() {
        module.set(SurfMicroserviceModule.MICROSERVICE)
    }
}