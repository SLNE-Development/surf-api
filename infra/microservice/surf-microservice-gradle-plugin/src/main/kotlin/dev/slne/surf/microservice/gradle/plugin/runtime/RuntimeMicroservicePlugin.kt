package dev.slne.surf.microservice.gradle.plugin.runtime

import dev.slne.surf.microservice.gradle.plugin.CommonMicroservicePlugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType

internal class RuntimeMicroservicePlugin : CommonMicroservicePlugin<RuntimeMicroserviceExtension>(
    platformName = "runtime"
) {
    override val extensionClass = RuntimeMicroserviceExtension::class.java

    override fun Project.afterEvaluate0(extension: RuntimeMicroserviceExtension) {
        extension.module.orNull?.let { moduleDependency ->
            val module = moduleDependency.module

            if (module == SurfMicroserviceRuntimeModule.MICROSERVICE.module) {
                pluginManager.apply("application")

                tasks.withType<Jar> {
                    manifest {
                        attributes(
                            mapOf(
                                "Main-Class" to "dev.slne.surf.microservice.runtime.microservice.MicroserviceLauncherKt"
                            )
                        )
                    }
                }

//                tasks.named("shadowJar", ShadowJar::class.java).configure {
//                    manifest {
//                        attributes(
//                            mapOf(
//                                "Main-Class" to "dev.slne.surf.microservice.api.MicroserviceLauncherKt"
//                            )
//                        )
//                    }
//                }
            }
        }
    }
}