package dev.slne.surf.microservice.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class SurfMicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create("surfMicroservice", SurfMicroserviceExtension::class.java)

        pluginManager.apply("application")
    }
}