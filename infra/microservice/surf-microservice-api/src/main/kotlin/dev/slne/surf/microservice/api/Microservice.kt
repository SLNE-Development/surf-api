package dev.slne.surf.microservice.api

import dev.slne.surf.microservice.api.util.InternalMicroserviceApi
import kotlin.reflect.full.findAnnotation

abstract class Microservice {
    private val annotation = this::class.findAnnotation<SurfMicroservice>()
        ?: throw IllegalStateException("Microservice class must be annotated with @SurfMicroservice")

    val id: String by annotation::name

    @InternalMicroserviceApi
    suspend fun bootstrap() {

    }

    @InternalMicroserviceApi
    suspend fun disable() {

    }

    open suspend fun onBootstrap() {}
    open suspend fun onDisable() {}
}