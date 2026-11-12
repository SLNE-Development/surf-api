package dev.slne.surf.microservice.gradle.run

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider

internal const val RUN_MICROSERVICE_TASK_NAME = "runMicroservice"
private const val RUN_DIRECTORY = "run"

/**
 * Registers `runMicroservice`, which runs [applicationJar] with `java -jar` on the project's Java toolchain.
 *
 * The working directory is `run/` in the project directory, so configuration and data survive `clean`.
 * Standard input is forwarded for the microservice console.
 */
internal fun Project.registerRunMicroservice(applicationJar: TaskProvider<out AbstractArchiveTask>) {
    val runDirectory = layout.projectDirectory.dir(RUN_DIRECTORY).asFile
    val jarFile = applicationJar.flatMap { it.archiveFile }
    val launcher = extensions.getByType<JavaToolchainService>()
        .launcherFor(extensions.getByType<JavaPluginExtension>().toolchain)

    tasks.register<Exec>(RUN_MICROSERVICE_TASK_NAME) {
        group = "application"
        description = "Builds the microservice jar and runs it in $RUN_DIRECTORY/."
        dependsOn(applicationJar)

        workingDir = runDirectory
        standardInput = System.`in`
        doFirst {
            runDirectory.mkdirs()
            executable = launcher.get().executablePath.asFile.absolutePath
        }
        argumentProviders += CommandLineArgumentProvider {
            listOf("-jar", jarFile.get().asFile.absolutePath)
        }
    }
}
