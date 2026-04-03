package dev.slne.surf.api.gen.data

import kotlinx.serialization.Serializable

@Serializable
data class GenericRegistry(
    val entries: Map<String, RegistryEntry>
)