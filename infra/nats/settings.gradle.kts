pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public/") { name = "public" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}

rootProject.name = "surf-nats"

include("surf-nats-api")
include("surf-nats-core")
include("surf-nats-paper")
include("surf-nats-velocity")
include("surf-nats-minestom")
include("surf-nats-standalone")
