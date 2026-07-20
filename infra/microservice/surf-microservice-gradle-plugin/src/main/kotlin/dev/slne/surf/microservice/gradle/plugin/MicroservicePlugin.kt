package dev.slne.surf.microservice.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

@Suppress("unused")
abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<MicroserviceExtension>("surfMicroservice")
        val runnableTaskNameProvider = objects.property(String::class.java).convention("shadowJar")
        val runnableArtifactPathProvider = objects.property(String::class.java).convention(
            "build/libs/${name}-${version}-all.jar"
        )
        val dockerBuildContextDirectory = rootProject.layout.projectDirectory.asFile
        val generateDockerfile = tasks.register<GenerateMicroserviceDockerfile>("generateMicroserviceDockerfile") {
            group = "distribution"
            description = "Generates a dependency-aware Dockerfile for a runnable Surf microservice."
            outputFile.set(extension.docker.outputFile)
            overwrite.set(extension.docker.overwrite)
            baseImage.set(extension.docker.baseImage)
            builderImage.set(extension.docker.builderImage)
            runnableTaskName.set(runnableTaskNameProvider)
            runnableArtifactPath.set(runnableArtifactPathProvider)
            runtimeDependencies.convention(emptySet())
        }

        extension.docker.outputFile.convention(layout.projectDirectory.file("Dockerfile"))
        extension.docker.overwrite.convention(false)
        extension.docker.baseImage.convention("eclipse-temurin:21-jre")
        extension.docker.builderImage.convention("eclipse-temurin:21-jdk")

        pluginManager.withPlugin("java") {
            val java = extensions.getByType<JavaPluginExtension>()
            extension.docker.baseImage.convention(
                java.toolchain.languageVersion
                    .map { "eclipse-temurin:${it.asInt()}-jre" }
                    .orElse("eclipse-temurin:21-jre")
            )
            extension.docker.builderImage.convention(
                java.toolchain.languageVersion
                    .map { "eclipse-temurin:${it.asInt()}-jdk" }
                    .orElse("eclipse-temurin:21-jdk")
            )

            val runtimeClasspath = configurations.named("runtimeClasspath")
            generateDockerfile.configure {
                runtimeDependencies.set(
                    runtimeClasspath.flatMap { configuration ->
                        configuration.incoming.resolutionResult.rootComponent.map(::runtimeCoordinates)
                    }
                )
            }
        }

        afterEvaluate {
            if ("shadowJar" in tasks.names) {
                val runnableArchive = tasks.named<AbstractArchiveTask>("shadowJar")
                runnableArtifactPathProvider.set(
                    runnableArchive.flatMap { it.archiveFile }.map { archive ->
                        archive.asFile.relativeTo(dockerBuildContextDirectory).invariantSeparatorsPath
                    }
                )
            }

            extension.module.orNull?.let { moduleDependency ->
                val apiModule = moduleDependency.apiModule
                val runtimeModule = moduleDependency.runtimeModule
                val projectModification = moduleDependency.moduleProjectModification

                dependencies {
                    "compileOnlyApi"("dev.slne.surf.microservice:${apiModule}:+")
                    "runtimeOnly"("dev.slne.surf.microservice:${runtimeModule}:+")
                }

                projectModification()
            }

            extension.rabbitSettings.orNull?.let { rabbitSettings ->
                val moduleName = rabbitSettings.rabbitModule.module
                val applyServerRuntimeDependency = rabbitSettings.applyRabbitServerRuntimeDependency
                val applyKspProcessor = rabbitSettings.applyRabbitKspProcessor

                dependencies {
                    "compileOnlyApi"("dev.slne.surf.rabbitmq:surf-rabbitmq-$moduleName:+")

                    if (applyKspProcessor) {
                        "ksp"("dev.slne.surf.rabbitmq:surf-rabbitmq-ksp:+") // ksp is provided by surf-api
                    }

                    if (applyServerRuntimeDependency) {
                        "runtimeOnly"("dev.slne.surf.rabbitmq:surf-rabbitmq-server:+")
                    }
                }
            }
        }
    }
}

private fun runtimeCoordinates(root: ResolvedComponentResult): Set<String> {
    val coordinates = sortedSetOf<String>()
    val visited = mutableSetOf<Any>()

    fun visit(component: ResolvedComponentResult) {
        if (!visited.add(component.id)) return
        component.moduleVersion?.let { module ->
            coordinates += "${module.group}:${module.name}"
        }
        component.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .forEach { dependency -> visit(dependency.selected) }
    }

    visit(root)
    return coordinates
}
