pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "26.1+"
}

rootProject.name = "surf-redis"

include("surf-redis-api")
include("surf-redis-paper")
include("surf-redis-velocity")
include("surf-redis-core")