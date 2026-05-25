package dev.slne.surf.microservice.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import org.jetbrains.kotlin.gradle.utils.RUNTIME_ONLY

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
                    add(COMPILE_ONLY, "dev.slne.surf.microservice:${apiModule}:+")
                    add(RUNTIME_ONLY, "dev.slne.surf.microservice:${runtimeModule}:+")
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
                        add(RUNTIME_ONLY, "dev.slne.surf.rabbitmq:surf-rabbitmq-server:+")
                    }
                }
            }
        }
    }
}