plugins {
    `java-library`
    `kotlin-dsl`

    id("com.gradle.plugin-publish") version "1.3.0"
    alias(libs.plugins.maven.repo.auth)
}

group = findProperty("group") as String
version = (findProperty("mcVersion") as String) + "-1.0.31-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val pluginDependencies = listOf(
    libs.kotlin.gradle.plugin,
    libs.kotlin.all.open,
    libs.kotlin.no.arg,
    libs.maven.repo.auth,
    libs.shadow.gradle.plugin,
    libs.run.paper.gradle.plugin,
    libs.plugin.yml.paper.gradle.plugin,
)

dependencies {
    compileOnly(gradleApi())
    pluginDependencies.forEach { dep -> implementation(dep) }
// https://mvnrepository.com/artifact/com.squareup/javapoet
    implementation("com.squareup:javapoet:1.13.0")
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