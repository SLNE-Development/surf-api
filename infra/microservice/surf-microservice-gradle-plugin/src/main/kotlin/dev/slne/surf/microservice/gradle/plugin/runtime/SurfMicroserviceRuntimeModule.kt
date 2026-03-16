package dev.slne.surf.microservice.gradle.plugin.runtime

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency
import dev.slne.surf.microservice.gradle.plugin.SurfApiPlugin

enum class SurfMicroserviceRuntimeModule(
    module: String,
    override val surfApiPlugin: SurfApiPlugin
) : ModuleDependency {
    MICROSERVICE("microservice", SurfApiPlugin.STANDALONE),
    COMMON("common", SurfApiPlugin.CORE),
    PAPER("paper", SurfApiPlugin.PAPER_PLUGIN),
    VELOCITY("velocity", SurfApiPlugin.VELOCITY);

    override val module: String = "surf-microservice-runtime-$module"
}