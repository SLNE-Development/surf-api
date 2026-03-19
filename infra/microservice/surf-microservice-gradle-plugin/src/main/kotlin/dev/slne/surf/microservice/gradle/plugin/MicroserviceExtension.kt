package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule
import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModuleSettings
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

abstract class MicroserviceExtension(objects: ObjectFactory) {
    internal val relocation = objects.property<String>()

    internal val module = objects.property<SurfMicroserviceModule>()
    internal val rabbitSettings = objects.property<RabbitModuleSettings>()

    fun withRelocation(relocation: String) {
        this.relocation.set(relocation)
        this.relocation.finalizeValue()
    }

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

        this.rabbitSettings.finalizeValue()
    }

    fun withMicroserviceApi() {
        this.module.set(SurfMicroserviceModule.MICROSERVICE)
        this.module.finalizeValue()
    }

    fun withCommonApi() {
        this.module.set(SurfMicroserviceModule.COMMON)
        this.module.finalizeValue()
    }

    fun withClientCommonApi() {
        this.module.set(SurfMicroserviceModule.CLIENT_COMMON)
        this.module.finalizeValue()
    }

    fun withClientPaperApi() {
        this.module.set(SurfMicroserviceModule.CLIENT_PAPER)
        this.module.finalizeValue()
    }

    fun withClientVelocityApi() {
        this.module.set(SurfMicroserviceModule.CLIENT_VELOCITY)
        this.module.finalizeValue()
    }
}