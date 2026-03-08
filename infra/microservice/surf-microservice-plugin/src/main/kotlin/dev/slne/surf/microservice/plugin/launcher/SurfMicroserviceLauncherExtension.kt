package dev.slne.surf.microservice.plugin.launcher

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

class SurfMicroserviceLauncherExtension @Inject constructor(
    objects: ObjectFactory
) {
    val mainClass: Property<String> = objects.property(String::class.java)
    val serviceProject: Property<Project> = objects.property(Project::class.java)
}