pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://reposilite.slne.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}

rootProject.name = "surf-microservice"

include("surf-microservice-api")
include("surf-microservice-runtime")
include("surf-microservice-database")
include("surf-microservice-processor")
include("surf-microservice-gradle-plugin")
