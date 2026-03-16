package dev.slne.surf.microservice.gradle.plugin

import org.gradle.api.provider.Property

abstract class CommonMicroserviceExtension {
    abstract val module: Property<out ModuleDependency>
}