package dev.slne.surf.microservice.gradle.docker

import dev.slne.surf.microservice.gradle.MicroserviceDockerExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType

private const val DEFAULT_JAVA_VERSION = 25
private const val DEFAULT_COMMAND_CLIENT_BUILDER_IMAGE = "golang:1.26-alpine"
private const val DEFAULT_MEMORY_BUDGET = "1g"

/**
 * JVM options measured for the microservices: G1 returns unused heap after a minute of low activity
 * (`G1PeriodicGCInterval`) and native memory is trimmed every minute, so idle services shrink after load peaks.
 * Two processors keep GC, JIT and coroutine pools small on hosts with many cores.
 */
private val DEFAULT_JVM_OPTIONS = listOf(
    "-XX:+UseG1GC",
    "-XX:+UseCompactObjectHeaders",
    "-XX:MaxRAMPercentage=60.0",
    "-XX:ActiveProcessorCount=2",
    "-XX:G1PeriodicGCInterval=60000",
    "-XX:TrimNativeHeapInterval=60000",
    "-XX:+ExitOnOutOfMemoryError",
)

/**
 * Sets the defaults of [docker]. [applyJavaDockerConventions] replaces the image defaults once the `java` plugin is
 * applied.
 *
 * `jvmOptions` is set instead of given a convention, so that `add` in the DSL appends to the defaults.
 */
internal fun Project.applyDockerConventions(docker: MicroserviceDockerExtension) {
    docker.outputFile.convention(layout.projectDirectory.file("Dockerfile"))
    docker.overwrite.convention(false)
    docker.baseImage.convention(runtimeImage(DEFAULT_JAVA_VERSION))
    docker.builderImage.convention(builderImage(DEFAULT_JAVA_VERSION))
    docker.commandClientBuilderImage.convention(DEFAULT_COMMAND_CLIENT_BUILDER_IMAGE)
    docker.jvmOptions.set(DEFAULT_JVM_OPTIONS)
    docker.memoryBudget.convention(DEFAULT_MEMORY_BUDGET)
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
