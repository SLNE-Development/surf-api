pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.surfapi.gradle.settings") version "1.21.11+"
}

rootProject.name = "surf-microservice"

// Api
include("surf-microservice-api:surf-microservice-api-common")
include("surf-microservice-api:surf-microservice-api-microservice")
include("surf-microservice-api:surf-microservice-api-runtime:surf-microservice-api-runtime-common")
include("surf-microservice-api:surf-microservice-api-runtime:surf-microservice-api-runtime-paper")
include("surf-microservice-api:surf-microservice-api-runtime:surf-microservice-api-runtime-velocity")

// Core
include("surf-microservice-core:surf-microservice-core-common")
include("surf-microservice-core:surf-microservice-core-microservice")
include("surf-microservice-core:surf-microservice-core-runtime:surf-microservice-core-runtime-common")
include("surf-microservice-core:surf-microservice-core-runtime:surf-microservice-core-runtime-paper")
include("surf-microservice-core:surf-microservice-core-runtime:surf-microservice-core-runtime-velocity")

// Runtime
include("surf-microservice-runtime:surf-microservice-runtime-common")
include("surf-microservice-runtime:surf-microservice-runtime-microservice")
include("surf-microservice-runtime:surf-microservice-runtime-paper")
include("surf-microservice-runtime:surf-microservice-runtime-velocity")

// Gradle Plugin
include("surf-microservice-gradle-plugin")