package dev.slne.surf.microservice.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<MicroserviceExtension>("surfMicroservice")

        afterEvaluate {
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