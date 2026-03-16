package dev.slne.surf.microservice.gradle.plugin.core

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency

enum class SurfMicroserviceCoreModule(
    module: String,
) : ModuleDependency {
    COMMON("common"),
    MICROSERVICE("microservice"),
    RUNTIME_COMMON("runtime-common"),
    RUNTIME_PAPER("runtime-paper"),
    RUNTIME_VELOCITY("runtime-velocity");

    override val module: String = "surf-microservice-core-$module"
}