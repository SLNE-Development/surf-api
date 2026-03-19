package dev.slne.surf.microservice.gradle.plugin

enum class SurfMicroserviceModule(
    override val apiModule: String,
    override val runtimeModule: String
) : ModuleDependency {
    COMMON(
        apiModule = "surf-microservice-api-common",
        runtimeModule = "surf-microservice-core"
    ),
    MICROSERVICE(
        apiModule = "surf-microservice-api-microservice",
        runtimeModule = "surf-microservice-microservice"
    ),
    CLIENT_COMMON(
        apiModule = "surf-microservice-api-client-common",
        runtimeModule = "surf-microservice-client-common"
    ),
    CLIENT_PAPER(
        apiModule = "surf-microservice-api-client-paper",
        runtimeModule = "surf-microservice-client-paper"
    ),
    CLIENT_VELOCITY(
        apiModule = "surf-microservice-api-client-velocity",
        runtimeModule = "surf-microservice-client-velocity"
    );
}