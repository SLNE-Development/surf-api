package dev.slne.surf.microservice.gradle.plugin.api

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency
import dev.slne.surf.microservice.gradle.plugin.SurfApiPlugin

enum class SurfMicroserviceApiModule(
    module: String,
    override val surfApiPlugin: SurfApiPlugin
) : ModuleDependency {
    COMMON("common", SurfApiPlugin.CORE),
    MICROSERVICE("microservice", SurfApiPlugin.CORE),
    RUNTIME_COMMON("runtime-common", SurfApiPlugin.CORE),
    RUNTIME_PAPER("runtime-paper", SurfApiPlugin.PAPER_RAW),
    RUNTIME_VELOCITY("runtime-velocity", SurfApiPlugin.CORE);

    override val module: String = "surf-microservice-api-$module"
}