package dev.slne.surf.microservice.gradle.plugin

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.microservice.gradle.generated.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<MicroserviceExtension>("surfMicroservice")

        afterEvaluate {
            extension.module.orNull?.let { moduleDependency ->
                val apiModule = moduleDependency.apiModule
                val runtimeModule = moduleDependency.runtimeModule

                val relocation = extension.relocation.orNull ?: run {
                    throw IllegalArgumentException("Relocation must be specified for the microservice plugin. Use the withRelocation() method in the extension to set it.")
                }

                dependencies.add(
                    "compileOnlyApi",
                    "dev.slne.surf.microservice:${apiModule}:${Constants.MINECRAFT_VERSION}+"
                )

                dependencies.add(
                    "runtimeOnly",
                    "dev.slne.surf.microservice:${runtimeModule}:${Constants.MINECRAFT_VERSION}+"
                )

                tasks.named("shadowJar", ShadowJar::class.java) {
                    relocate("dev.slne.surf.microservice", relocation)
                }
            }

            extension.rabbitSettings.orNull?.let { rabbitSettings ->
                val moduleName = rabbitSettings.rabbitModule.module
                val applyServerRuntimeDependency = rabbitSettings.applyRabbitServerRuntimeDependency

                dependencies.add(
                    "compileOnlyApi",
                    "dev.slne.surf.rabbitmq:surf-rabbitmq-$moduleName:+"
                )

                if (applyServerRuntimeDependency) {
                    dependencies.add(
                        "runtimeOnly",
                        "dev.slne.surf.rabbitmq:surf-rabbitmq-server:+"
                    )
                }
            }
        }
    }
}