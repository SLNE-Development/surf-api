package dev.slne.surf.api.shared.internal.nms

import kotlinx.serialization.Serializable

@Serializable
data class NmsProviderMeta(
    val version: NmsVersion,
    val implementation: String
)