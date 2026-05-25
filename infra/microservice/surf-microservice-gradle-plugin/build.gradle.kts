plugins {
    id("dev.slne.surf.api.gradle.core")
    id("com.github.gmazzo.buildconfig") version "6.0.9"

    idea
    `kotlin-dsl`
}

dependencies {
    implementation("dev.slne.surf.api:surf-api-gradle-plugin:+")
}

gradlePlugin {
    plugins {
        create("dev.slne.surf.microservice.gradle.plugin") {
            id = "dev.slne.surf.microservice"
            implementationClass = "dev.slne.surf.microservice.gradle.plugin.MicroservicePlugin"
        }
    }

    publishing {
        repositories {
            maven("https://reposilite.slne.dev/releases") {
                credentials {
                    username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                    password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
                }
            }
        }
    }
}

buildConfig {
    forClass("dev.slne.surf.microservice.gradle.generated", "Constants") {
        buildConfigField("MINECRAFT_VERSION", providers.gradleProperty("mcVersion"))
        buildConfigField("SURF_MICROSERVICE_VERSION", "+")
        buildConfigField("SURF_MICROSERVICE_FULL_VERSION", providers.gradleProperty("version"))
    }
}