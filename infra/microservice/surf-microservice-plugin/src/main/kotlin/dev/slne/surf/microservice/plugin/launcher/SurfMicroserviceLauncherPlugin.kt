package dev.slne.surf.microservice.plugin.launcher

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.microservice.plugin.SurfMicroserviceExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class SurfMicroserviceLauncherPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create(
            "surfMicroserviceLauncher",
            SurfMicroserviceLauncherExtension::class.java
        )

        afterEvaluate {
            val serviceProject = extension.serviceProject.get()
            val serviceExtension = serviceProject.extensions
                .getByType(SurfMicroserviceExtension::class.java)

            val serviceMainClass = serviceExtension.mainClass.get()
            val serviceDependencies = serviceExtension.dependencies.get().toList()

            val copyServiceJar = tasks.register("copyServiceJar", Copy::class.java) { copy ->
                copy.dependsOn(serviceProject.tasks.named("jar"))
                copy.from(serviceProject.layout.buildDirectory.file("libs/${serviceProject.name}.jar"))
                copy.into(layout.buildDirectory.dir("launcher"))
                copy.rename { "service.jar.disabled" }
            }

            val generateMetadata = project.tasks
                .register("generateMicroserviceMetadata") { generate ->
                    val outputFileProvider = project.layout.buildDirectory
                        .file("resources/main/META-INF/microservice.json")

                    generate.outputs.file(outputFileProvider)

                    generate.doLast {
                        val outputFile = outputFileProvider.get().asFile

                        outputFile.parentFile.mkdirs()
                        outputFile.writeText(
                            """
                            {
                                "mainClass": "$serviceMainClass",
                                "dependencies": [${serviceDependencies.joinToString(",") { "\"$it\"" }}]
                            }
                            """.trimIndent()
                        )
                    }
                }

            project.tasks.named("processResources") { process ->
                process.dependsOn(generateMetadata)
            }

            project.tasks.named("shadowJar", ShadowJar::class.java) { shadowJar ->
                shadowJar.dependsOn(copyServiceJar)
                shadowJar.from(project.layout.buildDirectory.file("launcher/service.jar.disabled"))

                shadowJar.manifest.attributes["Main-Class"] =
                    "dev.slne.surf.microservice.launcher.MicroserviceLauncher"
            }
        }
    }
}