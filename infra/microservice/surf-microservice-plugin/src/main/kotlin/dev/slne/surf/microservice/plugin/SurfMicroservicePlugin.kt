package dev.slne.surf.microservice.plugin

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class SurfMicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("application")

        target.tasks.named("shadowJar", ShadowJar::class.java).configure { shadow ->
            shadow.manifest { manifest ->
                manifest.attributes(
                    mapOf(
                        "Main-Class" to "dev.slne.surf.microservice.api.MicroserviceLauncherKt"
                    )
                )
            }
        }
    }
}