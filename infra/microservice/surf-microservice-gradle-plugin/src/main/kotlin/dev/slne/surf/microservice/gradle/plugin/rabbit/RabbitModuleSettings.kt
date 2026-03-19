package dev.slne.surf.microservice.gradle.plugin.rabbit

data class RabbitModuleSettings(
    val rabbitModule: RabbitModule,
    val applyRabbitServerRuntimeDependency: Boolean = false,
)
