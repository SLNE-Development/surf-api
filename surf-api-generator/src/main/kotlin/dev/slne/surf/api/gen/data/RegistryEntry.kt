package dev.slne.surf.api.gen.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistryEntry(
    @SerialName("protocol_id") val protocolId: Int,
)