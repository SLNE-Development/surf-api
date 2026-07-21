package dev.slne.surf.microservice.gradle.plugin

enum class SurfMicroserviceModule(
    internal val apiArtifactId: String,
    internal val runtimeArtifactId: String,
) {
    COMMON(
        apiArtifactId = "surf-microservice-api-common",
        runtimeArtifactId = "surf-microservice-core",
    ),

    MICROSERVICE(
        apiArtifactId = "surf-microservice-api-microservice",
        runtimeArtifactId = "surf-microservice-microservice",
    ),

    CLIENT_COMMON(
        apiArtifactId = "surf-microservice-api-client-common",
        runtimeArtifactId = "surf-microservice-client-common",
    ),

    CLIENT_PAPER(
        apiArtifactId = "surf-microservice-api-client-paper",
        runtimeArtifactId = "surf-microservice-client-paper",
    ),

    CLIENT_VELOCITY(
        apiArtifactId = "surf-microservice-api-client-velocity",
        runtimeArtifactId = "surf-microservice-client-velocity",
    ),
}