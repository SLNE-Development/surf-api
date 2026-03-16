package dev.slne.surf.microservice.gradle.plugin.client

import dev.slne.surf.microservice.gradle.plugin.ModuleDependency

enum class SurfMicroserviceModule(
    apiModule: String,
    runtimeModule: String = apiModule,
) : ModuleDependency {
    COMMON("common", "common"),
    CLIENT_COMMON("client-common"),
    CLIENT_PAPER("client-paper"),
    CLIENT_VELOCITY("client-velocity"),
    MICROSERVICE("microservice");

    override val apiModule: String = "surf-microservice-api-$apiModule"
    override val runtimeModule: String = "surf-microservice-$runtimeModule"
}