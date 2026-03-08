package dev.slne.surf.microservice.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class SurfMicroserviceExtension @Inject constructor(
    objects: ObjectFactory
) {
    val mainClass: Property<String> = objects.property(String::class.java)
    val dependencies: ListProperty<String> = objects.listProperty(String::class.java)
}