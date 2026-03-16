package dev.slne.surf.microservice.gradle.plugin.utils

data class RabbitModuleSettings(
    val rabbitModule: RabbitModule,
    val applyRabbitServerRuntimeDependency: Boolean = false,
)
