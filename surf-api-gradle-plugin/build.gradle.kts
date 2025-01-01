plugins {
    `java-library`
    `kotlin-dsl`

    id("com.gradle.plugin-publish") version "1.3.0"
    alias(libs.plugins.maven.repo.auth)
}

group = findProperty("group") as String
version = (findProperty("mcVersion") as String) + "-1.0.20-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())

    val pluginDependencies = listOf(
        libs.kotlin.gradle.plugin,
        libs.kotlin.all.open,
        libs.kotlin.no.arg,
        libs.maven.repo.auth,
        libs.shadow.gradle.plugin,
        libs.run.paper.gradle.plugin,
        libs.plugin.yml.paper.gradle.plugin,
    )

    pluginDependencies.forEach { dep -> implementation(dep) }
}

gradlePlugin {
    plugins {
        create("gradle") {
            id = "dev.slne.surf.surfapi.gradle"
            implementationClass = "dev.slne.surf.surfapi.gradle.SurfApiPlugin"
        }
    }

    publishing {
        repositories {
            maven("https://repo.slne.dev/repository/maven-unsafe/") { name = "maven-unsafe" }
        }
    }
}