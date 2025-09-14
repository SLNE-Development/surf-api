package dev.slne.surf.api.gen.data

import kotlinx.serialization.Serializable

@Serializable
data class AdvancementCategory(
    val name: String,
    val entries: List<String>,
    val children: List<AdvancementCategory>,
)