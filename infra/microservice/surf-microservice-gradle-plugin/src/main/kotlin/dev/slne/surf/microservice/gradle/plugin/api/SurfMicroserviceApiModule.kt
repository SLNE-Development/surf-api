package dev.slne.surf.microservice.gradle.plugin.api

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency

enum class SurfMicroserviceApiModule(
    module: String,
) : ModuleDependency {
    COMMON("common"),
    MICROSERVICE("microservice"),
    RUNTIME_COMMON("runtime-common"),
    RUNTIME_PAPER("runtime-paper"),
    RUNTIME_VELOCITY("runtime-velocity");

    override val module: String = "surf-microservice-api-$module"
}