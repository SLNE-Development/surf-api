package dev.slne.surf.microservice.launcher

import kotlinx.serialization.Serializable

@Serializable
data class MicroserviceMetadata(
    val mainClass: String,
    val dependencies: List<String>
)
