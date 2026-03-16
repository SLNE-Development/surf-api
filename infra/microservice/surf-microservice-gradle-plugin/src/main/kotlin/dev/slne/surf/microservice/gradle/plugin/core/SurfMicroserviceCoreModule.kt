package dev.slne.surf.microservice.gradle.plugin.core

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency

enum class SurfMicroserviceCoreModule(
    module: String,
) : ModuleDependency {
    COMMON("common");

    override val apiModule: String = "surf-microservice-core-$module"
}