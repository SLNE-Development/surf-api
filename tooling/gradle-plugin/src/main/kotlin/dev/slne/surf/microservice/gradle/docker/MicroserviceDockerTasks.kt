package dev.slne.surf.microservice.gradle.docker

import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import dev.slne.surf.microservice.gradle.MicroserviceDockerExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.register

internal const val GENERATE_DOCKERFILE_TASK_NAME = "generateMicroserviceDockerfile"
internal const val PREPARE_DOCKER_ARTIFACT_TASK_NAME = "prepareMicroserviceDockerArtifact"
private const val GENERATE_COMMAND_CLIENT_SOURCE_TASK_NAME = "generateMicroserviceCommandClientSource"

private const val COMMAND_CLIENT_SOURCE_RESOURCE_PATH = "/microservicectl/main.go"
private const val COMMAND_CLIENT_SOURCE_OUTPUT_PATH = "generated/microservicectl/main.go"
private const val DOCKER_ARTIFACT_DIRECTORY = "docker"

/**
 * Registers the Docker tasks for the runnable [applicationJar].
 *
 * The Dockerfile builds the image from the root project directory as context: its first stage runs
 * `prepareMicroserviceDockerArtifact`, which copies the jar and the `microservicectl` source below this
 * project's build directory.
 */
internal fun Project.registerMicroserviceDockerTasks(
    docker: MicroserviceDockerExtension,
    applicationJar: TaskProvider<out AbstractArchiveTask>,
) {
    val commandClientSource = layout.buildDirectory.file(COMMAND_CLIENT_SOURCE_OUTPUT_PATH)
    val generateCommandClientSource = tasks.register<GenerateMicroserviceCommandClientSource>(
        GENERATE_COMMAND_CLIENT_SOURCE_TASK_NAME
    ) {
        group = "build"
        description = "Generates the source of the native microservicectl command client."
        sourceText.set(providers.provider { loadCommandClientSource() })
        outputFile.set(commandClientSource)
    }

    val prepareArtifact = tasks.register<Sync>(PREPARE_DOCKER_ARTIFACT_TASK_NAME) {
        group = "distribution"
        description = "Prepares the runnable microservice jar for Docker."
        dependsOn(generateCommandClientSource)
        from(applicationJar.flatMap { it.archiveFile })
        into(layout.buildDirectory.dir(DOCKER_ARTIFACT_DIRECTORY))
        rename { DOCKER_APPLICATION_JAR_NAME }
    }

    val contextDirectory = rootProject.layout.projectDirectory.asFile
    // A plain string: mapping the TaskProvider would make the Dockerfile depend on building the jar.
    val prepareArtifactTaskPath = absoluteTaskPath(prepareArtifact.name)
    val prepareArtifactPath = providers.provider { prepareArtifactTaskPath }
    val dockerArtifactPath = layout.buildDirectory.file("$DOCKER_ARTIFACT_DIRECTORY/$DOCKER_APPLICATION_JAR_NAME")
        .map { artifact -> artifact.asFile.relativeToDockerContext(contextDirectory) }
    val commandClientSourcePath = commandClientSource
        .map { source -> source.asFile.relativeToDockerContext(contextDirectory) }

    tasks.register<Dockerfile>(GENERATE_DOCKERFILE_TASK_NAME) {
        group = "distribution"
        description = "Generates a Dockerfile for the microservice."
        destFile.set(docker.outputFile)

        configureMicroserviceDockerfile(
            overwrite = docker.overwrite,
            builderImage = docker.builderImage,
            commandClientBuilderImage = docker.commandClientBuilderImage,
            baseImage = docker.baseImage,
            jvmOptions = docker.jvmOptions,
            memoryBudget = docker.memoryBudget,
            runnableTaskPath = prepareArtifactPath,
            runnableArtifactPath = dockerArtifactPath,
            commandClientSourcePath = commandClientSourcePath,
        )
    }
}

private fun Project.absoluteTaskPath(taskName: String): String =
    if (path == Project.PATH_SEPARATOR) ":$taskName" else "$path:$taskName"

private fun loadCommandClientSource(): String {
    val stream = GenerateMicroserviceCommandClientSource::class.java
        .getResourceAsStream(COMMAND_CLIENT_SOURCE_RESOURCE_PATH)
        ?: throw GradleException(
            "Could not find the embedded microservicectl source at $COMMAND_CLIENT_SOURCE_RESOURCE_PATH"
        )

    return stream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() }
}
