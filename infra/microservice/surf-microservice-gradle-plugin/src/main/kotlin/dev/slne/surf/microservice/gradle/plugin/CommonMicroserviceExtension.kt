package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.plugin.utils.RabbitModule
import dev.slne.surf.microservice.gradle.plugin.utils.RabbitModuleSettings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class CommonMicroserviceExtension(
    objects: ObjectFactory
) {
    abstract val module: Property<out ModuleDependency>

    val rabbitSettings = objects.property<RabbitModuleSettings>()

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
}