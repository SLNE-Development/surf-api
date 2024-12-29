plugins {
    java
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    kotlin("plugin.serialization") version libs.versions.kotlinVersion
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup:javapoet:1.13.0")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation(libs.paper.api)

    implementation(project(":surf-api-core:surf-api-core-api"))
}

tasks.register<JavaExec>("generate") {
    dependsOn(tasks.check)
    mainClass.set("dev.slne.surf.api.gen.MainKt")
    classpath(sourceSets.main.map { it.runtimeClasspath })
    args(projectDir.toPath().resolve("generated").toString())
}