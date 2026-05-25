package dev.slne.surf.microservice.gradle.plugin.rabbit

internal data class RabbitModuleSettings(
    val rabbitModule: RabbitModule,
    val applyRabbitServerRuntimeDependency: Boolean = false,
    val applyRabbitKspProcessor: Boolean = true
)
