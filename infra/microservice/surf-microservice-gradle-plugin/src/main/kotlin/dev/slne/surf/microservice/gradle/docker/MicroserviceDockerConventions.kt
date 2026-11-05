package dev.slne.surf.microservice.gradle.docker

import dev.slne.surf.microservice.gradle.MicroserviceDockerExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType

private const val DEFAULT_JAVA_VERSION = 25
private const val DEFAULT_COMMAND_CLIENT_BUILDER_IMAGE = "golang:1.26-alpine"

internal fun Project.applyDockerConventions(docker: MicroserviceDockerExtension) {
    docker.outputFile.convention(layout.projectDirectory.file("Dockerfile"))
    docker.overwrite.convention(false)
    docker.baseImage.convention(runtimeImage(DEFAULT_JAVA_VERSION))
    docker.builderImage.convention(builderImage(DEFAULT_JAVA_VERSION))
    docker.commandClientBuilderImage.convention(DEFAULT_COMMAND_CLIENT_BUILDER_IMAGE)
}

/**
 * Derives the Java images from the project's toolchain, falling back to Java [DEFAULT_JAVA_VERSION].
 */
internal fun Project.applyJavaDockerConventions(docker: MicroserviceDockerExtension) {
    val languageVersion = extensions.getByType<JavaPluginExtension>().toolchain.languageVersion
        .map { version -> version.asInt() }
        .orElse(DEFAULT_JAVA_VERSION)

    docker.baseImage.convention(languageVersion.map { version -> runtimeImage(version) })
    docker.builderImage.convention(languageVersion.map { version -> builderImage(version) })
}

private fun runtimeImage(javaVersion: Int) = "eclipse-temurin:$javaVersion-jre"

private fun builderImage(javaVersion: Int) = "eclipse-temurin:$javaVersion-jdk"
