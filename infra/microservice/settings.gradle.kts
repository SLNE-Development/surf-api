pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "26+"
}

rootProject.name = "surf-microservice"

// Api
include("surf-microservice-api:surf-microservice-api-common")
include("surf-microservice-api:surf-microservice-api-microservice")
include("surf-microservice-api:surf-microservice-api-client:surf-microservice-api-client-common")
include("surf-microservice-api:surf-microservice-api-client:surf-microservice-api-client-paper")
include("surf-microservice-api:surf-microservice-api-client:surf-microservice-api-client-velocity")

// Core
include("surf-microservice-core")

// Client
include("surf-microservice-client:surf-microservice-client-common")
include("surf-microservice-client:surf-microservice-client-paper")
include("surf-microservice-client:surf-microservice-client-velocity")
include("surf-microservice-microservice")

// Gradle Plugin
include("surf-microservice-gradle-plugin")
