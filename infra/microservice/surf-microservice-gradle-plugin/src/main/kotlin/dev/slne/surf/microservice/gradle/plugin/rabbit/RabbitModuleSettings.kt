package dev.slne.surf.microservice.gradle.plugin.rabbit

internal data class RabbitModuleSettings(
    val module: RabbitModule,
    val includeServerRuntime: Boolean,
    val includeKspProcessor: Boolean,
)