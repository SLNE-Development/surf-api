plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.slne.surf"
version = "1.0.1"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("plugin") {
            id = "dev.slne.surf.surfapi"
            implementationClass = "dev.slne.surf.surfapi.SurfApiPlugin"
            displayName = "Surf API Plugin"
            description = "Internal helper plugin for the Surf API"
            tags = listOf("surf")
            website = "https://git.slne.dev/surf/surf-api/-/wikis/home"
            vcsUrl = "https://git.slne.dev/surf/surf-api"
        }
    }
}

dependencies {
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.4.0")
}

kotlin {
    jvmToolchain(17)
}