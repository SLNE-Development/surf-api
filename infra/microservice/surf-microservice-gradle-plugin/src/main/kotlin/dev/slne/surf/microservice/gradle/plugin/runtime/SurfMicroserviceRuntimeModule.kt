package dev.slne.surf.microservice.gradle.plugin.runtime

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency

enum class SurfMicroserviceRuntimeModule(
    module: String,
) : ModuleDependency {
    MICROSERVICE("microservice"),
    COMMON("common"),
    PAPER("paper"),
    VELOCITY("velocity");

    override val module: String = "surf-microservice-runtime-$module"
}