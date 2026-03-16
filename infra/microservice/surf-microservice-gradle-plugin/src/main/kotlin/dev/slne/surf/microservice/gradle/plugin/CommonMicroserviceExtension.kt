package dev.slne.surf.microservice.gradle.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

abstract class CommonMicroserviceExtension(
    objects: ObjectFactory
) {
    internal val module = objects.property<ModuleDependency>()
}