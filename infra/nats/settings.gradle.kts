pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public/") { name = "public" }
    }
    includeBuild("surf-nats-gradle-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}

rootProject.name = "surf-nats"

include("surf-nats-api")
include("surf-nats-core")

include("surf-nats-types:surf-nats-types-common")
include("surf-nats-types:surf-nats-types-paper")

include("surf-nats-platform:surf-nats-paper")
include("surf-nats-platform:surf-nats-velocity")
include("surf-nats-platform:surf-nats-minestom")
include("surf-nats-platform:surf-nats-standalone")

include("surf-nats-test:surf-nats-test-common")
include("surf-nats-test:surf-nats-test-fixtures")
include("surf-nats-test:surf-nats-test-fixtures-internal")
include("surf-nats-test:surf-nats-test-paper")
include("surf-nats-test:surf-nats-test-microservice")
