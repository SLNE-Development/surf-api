pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://reposilite.slne.dev/public/") { name = "public" }
        // TODO(v3 release): remove once surf-microservice 3.0.0 is published.
        mavenLocal {
            content { includeGroup("dev.slne.surf.microservice") }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "surf-nats-gradle-plugin"
