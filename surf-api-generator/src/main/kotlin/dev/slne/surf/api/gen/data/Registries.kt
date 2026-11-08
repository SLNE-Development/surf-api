package dev.slne.surf.api.gen.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Registries(
    @SerialName("minecraft:sound_event") val soundRegistry: GenericRegistry,
    @SerialName("minecraft:block") val blockTypeRegistry: GenericRegistry,
    @SerialName("minecraft:item") val itemTypeRegistry: GenericRegistry,
)