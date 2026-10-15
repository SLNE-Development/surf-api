package dev.slne.surf.microservice.gradle.plugin

import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.platform.common.CommonSurfExtension
import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModuleSettings
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.*

private const val MICROSERVICE_GROUP = "dev.slne.surf.microservice"
private const val RABBITMQ_GROUP = "dev.slne.surf.rabbitmq"
private const val SHADOW_JAR_TASK_NAME = "shadowJar"
private const val MICROSERVICE_MAIN_CLASS =
    "dev.slne.surf.microservice.runtime.microservice.MicroserviceLauncherKt"
private const val DEFAULT_BASE_IMAGE = "eclipse-temurin:25-jre"
private const val DEFAULT_BUILDER_IMAGE = "eclipse-temurin:25-jdk"
private const val DEFAULT_COMMAND_CLIENT_BUILDER_IMAGE = "golang:1.26-alpine"

private const val PREPARE_DOCKER_ARTIFACT_TASK_NAME = "prepareMicroserviceDockerArtifact"
private const val GENERATE_COMMAND_CLIENT_SOURCE_TASK_NAME =
    "generateMicroserviceCommandClientSource"

private const val COMMAND_CLIENT_SOURCE_RESOURCE_PATH = "/microservicectl/main.go"
private const val COMMAND_CLIENT_SOURCE_OUTPUT_PATH = "generated/microservicectl/main.go"

@Suppress("unused")
abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<MicroserviceExtension>("surfMicroservice")

        configureDockerConventions(extension)

        val commandClientSourceFile = layout.buildDirectory.file(COMMAND_CLIENT_SOURCE_OUTPUT_PATH)

        val generateCommandClientSource = tasks.register<GenerateMicroserviceCommandClientSource>(
            GENERATE_COMMAND_CLIENT_SOURCE_TASK_NAME
        ) {
            group = "build"

            description = "Generates the native microservice command client source."

            sourceText.set(
                providers.provider {
                    loadCommandClientSource()
                }
            )

            outputFile.set(commandClientSourceFile)
        }

        pluginManager.withPlugin("com.gradleup.shadow") {
            val shadowJar = tasks.named<ShadowJar>(SHADOW_JAR_TASK_NAME)

            shadowJar.configure {
                mergeServiceFiles()
                filesMatching("META-INF/services/**") {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            }

            tasks.register<Sync>(PREPARE_DOCKER_ARTIFACT_TASK_NAME) {
                group = "distribution"
                description = "Prepares the runnable microservice JAR for Docker."

                dependsOn(shadowJar, generateCommandClientSource)

                from(shadowJar.flatMap { it.archiveFile })
                into(layout.buildDirectory.dir("docker"))
                rename { DOCKER_APPLICATION_JAR_NAME }
            }
        }

        val runnableTaskPath = providers.provider {
            taskPath(PREPARE_DOCKER_ARTIFACT_TASK_NAME)
        }

        val runnableArtifactPath = layout.buildDirectory
            .file("docker/$DOCKER_APPLICATION_JAR_NAME")
            .map { artifact ->
                artifact.asFile.relativeToDockerContext(
                    rootProject.projectDir
                )
            }

        val commandClientSourcePath = commandClientSourceFile.map { sourceFile ->
            sourceFile.asFile.relativeToDockerContext(
                rootProject.projectDir
            )
        }

        tasks.register<Dockerfile>("generateMicroserviceDockerfile") {
            group = "distribution"
            description = "Generates a Dockerfile for the microservice application."

            destFile.set(extension.docker.outputFile)

            configureMicroserviceDockerfile(
                overwrite = extension.docker.overwrite,
                builderImage = extension.docker.builderImage,
                commandClientBuilderImage = extension.docker.commandClientBuilderImage,
                baseImage = extension.docker.baseImage,
                runnableTaskPath = runnableTaskPath,
                runnableArtifactPath = runnableArtifactPath,
                commandClientSourcePath = commandClientSourcePath
            )
        }

        pluginManager.withPlugin("java") {
            configureJavaDockerConventions(extension)
        }

        afterEvaluate {
            extension.module.orNull?.let { module ->
                configureMicroserviceModule(module)
                configureNatsIntegration(module)
            }

            extension.rabbitSettings.orNull?.let { rabbitSettings ->
                configureRabbitModule(rabbitSettings)
            }
        }
    }
}

private fun Project.configureDockerConventions(extension: MicroserviceExtension) {
    with(extension.docker) {
        outputFile.convention(layout.projectDirectory.file("Dockerfile"))
        overwrite.convention(false)
        baseImage.convention(DEFAULT_BASE_IMAGE)
        builderImage.convention(DEFAULT_BUILDER_IMAGE)
        commandClientBuilderImage.convention(DEFAULT_COMMAND_CLIENT_BUILDER_IMAGE)
    }
}

private fun Project.configureJavaDockerConventions(
    extension: MicroserviceExtension,
) {
    val languageVersion = extensions
        .getByType<JavaPluginExtension>()
        .toolchain
        .languageVersion

    extension.docker.baseImage.convention(
        languageVersion
            .map { version ->
                "eclipse-temurin:${version.asInt()}-jre"
            }
            .orElse(DEFAULT_BASE_IMAGE)
    )

    extension.docker.builderImage.convention(
        languageVersion
            .map { version ->
                "eclipse-temurin:${version.asInt()}-jdk"
            }
            .orElse(DEFAULT_BUILDER_IMAGE)
    )
}

private fun Project.configureMicroserviceModule(
    module: SurfMicroserviceModule,
) {
    dependencies {
        "compileOnlyApi"(
            "$MICROSERVICE_GROUP:${module.apiArtifactId}:+"
        )

        "runtimeOnly"(
            "$MICROSERVICE_GROUP:${module.runtimeArtifactId}:+"
        )
    }

    if (module != SurfMicroserviceModule.MICROSERVICE) {
        return
    }

    tasks.withType<ShadowJar>().configureEach {
        mainClass.set(MICROSERVICE_MAIN_CLASS)
    }
}

private fun Project.configureNatsIntegration(
    module: SurfMicroserviceModule,
) {
    if (module != SurfMicroserviceModule.MICROSERVICE) {
        return
    }

    val surfNatsEnabled = extensions.findByType<CommonSurfExtension>()
        ?.surfNatsEnabled
        ?.get() == true

    if (!surfNatsEnabled) {
        return
    }

    dependencies {
        "runtimeOnly"("$MICROSERVICE_GROUP:surf-microservice-nats:+")
    }
}

private fun Project.configureRabbitModule(
    settings: RabbitModuleSettings,
) {
    dependencies {
        "compileOnlyApi"(
            "$RABBITMQ_GROUP:" +
                    "surf-rabbitmq-${settings.module.artifactSuffix}:+"
        )

        if (settings.includeKspProcessor) {
            "ksp"(
                "$RABBITMQ_GROUP:surf-rabbitmq-ksp:+"
            )
        }

        if (settings.includeServerRuntime) {
            "runtimeOnly"(
                "$RABBITMQ_GROUP:surf-rabbitmq-server:+"
            )
        }
    }
}

private fun Project.taskPath(
    taskName: String,
): String {
    return if (path == ":") {
        ":$taskName"
    } else {
        "$path:$taskName"
    }
}

private fun loadCommandClientSource(): String {
    val stream =
        MicroservicePlugin::class.java.getResourceAsStream(COMMAND_CLIENT_SOURCE_RESOURCE_PATH)
            ?: throw GradleException(
                "Could not find the embedded microservice command client source at " +
                        COMMAND_CLIENT_SOURCE_RESOURCE_PATH
            )

    return stream
        .bufferedReader(Charsets.UTF_8)
        .use { reader ->
            reader.readText()
        }
}