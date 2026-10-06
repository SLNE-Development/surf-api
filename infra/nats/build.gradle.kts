buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public/") { name = "public" }
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
    }
}

plugins {
    id("dev.slne.surf.api.gradle.standalone")
    application
}

group = "dev.slne.surf.nats"
version = findProperty("version") as String

dependencies {
    implementation(libs.jnats)
    runtimeOnly(libs.bundles.log4j)
}

application {
    mainClass = "dev.slne.surf.nats.SurfNatsKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    workingDir = layout.projectDirectory.dir("run").asFile.apply { mkdirs() }
}
