package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.generated.Constants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized

abstract class CommonMicroservicePlugin<E : CommonMicroserviceExtension>(
    protected val platformName: String,
) : Plugin<Project> {
    protected abstract val extensionClass: Class<E>

    override fun apply(target: Project) = with(target) {
        val extension = extensions.create(
            "surfMicroservice${platformName.capitalized()}",
            extensionClass
        )

        afterEvaluate {
            extension.module.orNull?.let { moduleDependency ->
                val moduleName = moduleDependency.module

                dependencies.add(
                    "api",
                    "dev.slne.surf.microservice:${moduleName}:${Constants.MINECRAFT_VERSION}+"
                )
            }

            afterEvaluate0(extension)
        }
    }

    protected open fun Project.afterEvaluate0(extension: E) {

    }
}